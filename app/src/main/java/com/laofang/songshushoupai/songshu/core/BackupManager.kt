package com.laofang.songshushoupai.songshu.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.core.content.edit
import java.io.InputStream

data class WebDavConfig(val url: String = "", val username: String = "", val password: String = "")

object BackupManager {

    const val ERR_AUTH_FAIL = "auth_fail"
    const val ERR_NO_PERMISSION = "no_permission"
    const val ERR_CONNECTION_FAILED = "conn_failed"
    const val ERR_NETWORK_ERROR = "network_error"
    const val ERR_UPLOAD_AUTH_FAIL = "upload_auth_fail"
    const val ERR_UPLOAD_NO_PERMISSION = "upload_no_permission"
    const val ERR_UPLOAD_FAILED = "upload_failed"
    const val ERR_UPLOAD_FAILED_SIMPLE = "upload_failed_simple"
    const val ERR_REDIRECT_NO_LOCATION = "redirect_no_location"
    const val ERR_BACKUP_NOT_FOUND = "backup_not_found"
    const val ERR_DOWNLOAD_NO_PERMISSION = "download_no_permission"
    const val ERR_SERVER_RESPONSE = "server_response"
    const val ERR_DOWNLOADED_EMPTY = "downloaded_empty"
    const val ERR_NO_CONFIG_IN_ZIP = "no_config_in_zip"
    const val ERR_UNKNOWN_ERROR = "unknown_error"

    init {
        CookieHandler.setDefault(CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) })
    }

    fun saveWebDavConfig(ctx: Context, cfg: WebDavConfig) {
        ctx.getSharedPreferences("backup_config", Context.MODE_PRIVATE).edit {
            putString("webdav_url", cfg.url); putString("webdav_user", cfg.username); putString("webdav_pass", cfg.password)
        }
    }

    fun loadWebDavConfig(ctx: Context): WebDavConfig {
        val p = ctx.getSharedPreferences("backup_config", Context.MODE_PRIVATE)
        return WebDavConfig(p.getString("webdav_url", "") ?: "", p.getString("webdav_user", "") ?: "", p.getString("webdav_pass", "") ?: "")
    }

    private fun buildConfigJson(ctx: Context): String {
        val list = ImageDataManager.getImageList(ctx)
        val sel = ImageDataManager.getSelectedIndex(ctx)
        val arr = JSONArray()
        list.forEachIndexed { i, item ->
            arr.put(JSONObject().apply {
                put("index", i); put("name", item.name); put("path", item.filePath)
                put("type", item.type); put("cover", item.coverPath)
                put("crop", item.cropRect)
            })
        }
        val qrList = QrCodeDataManager.getQrList(ctx)
        val qrSel = QrCodeDataManager.getSelectedIndex(ctx)
        val qrArr = JSONArray()
        qrList.forEach { qr ->
            qrArr.put(JSONObject().apply {
                put("path", qr.path); put("name", qr.name); put("link", qr.link)
            })
        }
        val s = SettingsManager.loadSettings(ctx)
        return JSONObject().apply {
            put("selected_index", sel); put("version", 2); put("images", arr)
            put("show_qr_code", s.showQrCode)
            put("qr_codes", qrArr)
            put("qr_selected_index", qrSel)
        }.toString(2)
    }

    private fun writeImagesToZip(zos: ZipOutputStream, ctx: Context, list: List<ImageItem>) {
        list.forEachIndexed { idx, item ->
            if (item.filePath.isNotEmpty()) {
                val f = File(item.filePath)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("images/img_${idx}_${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
                }
            }
            if (item.coverPath.isNotEmpty()) {
                val f = File(item.coverPath)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("covers/cover_${idx}_${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
                }
            }
        }
        val qrDir = File(ctx.filesDir, "qrcodes")
        if (qrDir.exists()) {
            qrDir.listFiles()?.filter { it.isFile }?.forEach { qr ->
                zos.putNextEntry(ZipEntry("qrcodes/${qr.name}"))
                qr.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
            }
        }
    }

    fun exportToZip(ctx: Context, uri: Uri) {
        val list = ImageDataManager.getImageList(ctx)
        ctx.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                zos.putNextEntry(ZipEntry("config.json"))
                zos.write(buildConfigJson(ctx).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
                writeImagesToZip(zos, ctx, list)
            }
        }
    }

    private fun readZipEntries(input: InputStream): Quad<String?, MutableMap<String, ByteArray>, MutableMap<String, ByteArray>, MutableMap<String, ByteArray>> {
        var configJson: String? = null
        val imageMap = mutableMapOf<String, ByteArray>()
        val coverMap = mutableMapOf<String, ByteArray>()
        val qrMap = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val data = zis.readBytes()
                    when {
                        entry.name == "config.json" -> configJson = String(data, Charsets.UTF_8)
                        entry.name.startsWith("images/") -> imageMap[entry.name] = data
                        entry.name.startsWith("covers/") -> coverMap[entry.name] = data
                        entry.name.startsWith("qrcodes/") -> qrMap[entry.name] = data
                    }
                }
                zis.closeEntry(); entry = zis.nextEntry
            }
        }
        return Quad(configJson, imageMap, coverMap, qrMap)
    }

    private fun restoreFromEntries(ctx: Context, configJson: String, imageMap: Map<String, ByteArray>, coverMap: Map<String, ByteArray>, qrMap: Map<String, ByteArray>, prefix: String = "import"): Boolean {
        val cfg = JSONObject(configJson)
        val arr = cfg.getJSONArray("images")
        val sel = cfg.optInt("selected_index", 0)
        val imgDir = File(ctx.filesDir, "images").also { it.mkdirs() }
        val covDir = File(ctx.filesDir, "covers").also { it.mkdirs() }
        val qrDir = File(ctx.filesDir, "qrcodes").also { it.mkdirs() }
        val ts = System.currentTimeMillis()
        val newList = mutableListOf<ImageItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("name", "badge_${i + 1}")
            val oldPath = o.optString("path", "")
            val type = o.optString("type", "image")
            val oldCover = o.optString("cover", "")
            var newPath = ""
            if (oldPath.isNotEmpty()) {
                val fn = File(oldPath).name
                val key = imageMap.keys.find { it.endsWith(fn) }
                if (key != null) { val f = File(imgDir, "${prefix}_${ts}_${i}_$fn"); f.writeBytes(imageMap[key]!!); newPath = f.absolutePath }
            }
            var newCoverPath = ""
            if (oldCover.isNotEmpty()) {
                val fn = File(oldCover).name
                val key = coverMap.keys.find { it.endsWith(fn) }
                if (key != null) { val f = File(covDir, "${prefix}_${ts}_${i}_$fn"); f.writeBytes(coverMap[key]!!); newCoverPath = f.absolutePath }
            }
            newList.add(ImageItem(i, newPath, name, type, newCoverPath, o.optString("crop", "")))

        }
        ImageDataManager.restoreList(ctx, newList, sel)
        val qrArr = cfg.optJSONArray("qr_codes")
        if (qrArr != null && qrArr.length() > 0) {
            val qrSel = cfg.optInt("qr_selected_index", 0)
            val restoredQrList = mutableListOf<QrCodeItem>()
            for (i in 0 until qrArr.length()) {
                val o = qrArr.getJSONObject(i)
                val oldPath = o.optString("path", "")
                val name = o.optString("name", "二维码 ${i + 1}")
                val link = o.optString("link", "")
                var newPath = ""
                if (oldPath.isNotEmpty()) {
                    val fn = File(oldPath).name
                    val key = qrMap.keys.find { it.endsWith(fn) }
                    if (key != null) {
                        val f = File(qrDir, "${prefix}_${ts}_${i}_$fn")
                        f.writeBytes(qrMap[key]!!); newPath = f.absolutePath
                    }
                }
                restoredQrList.add(QrCodeItem(newPath, name, link))
            }
            QrCodeDataManager.restoreList(ctx, restoredQrList, qrSel)
        } else {
            val qrPath = cfg.optString("qr_code_path", "")
            if (qrPath.isNotEmpty()) {
                val fn = File(qrPath).name
                val key = qrMap.keys.find { it.endsWith(fn) }
                if (key != null) {
                    val restored = File(qrDir, "${prefix}_${ts}_$fn")
                    QrCodeDataManager.addItem(ctx, QrCodeItem(
                        restored.absolutePath,
                        cfg.optString("qr_code_name", ""),
                        cfg.optString("qr_code_link", "")
                    )
                    )
                }
            }
        }
        ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit {
            putBoolean("show_qr_code", cfg.optBoolean("show_qr_code", false))
        }
        return true
    }

    fun importFromZip(ctx: Context, uri: Uri): Boolean {
        return try {
            val (configJson, imageMap, coverMap, qrMap) = ctx.contentResolver.openInputStream(uri)?.use { readZipEntries(it) }
                ?: return false
            if (configJson == null) return false
            restoreFromEntries(ctx, configJson, imageMap, coverMap, qrMap)
        } catch (_: Exception) { false }
    }

    fun webdavUpload(ctx: Context, cfg: WebDavConfig): String? = uploadZip(ctx, cfg)
    fun webdavDownload(ctx: Context, cfg: WebDavConfig): String? = downloadRestore(ctx, cfg)
    suspend fun webdavTestConnection(cfg: WebDavConfig): String? = withContext(Dispatchers.IO) {
        try {
            val conn = httpConn(fileUrl(cfg))
            try {
                conn.requestMethod = "HEAD"
                basicAuth(conn, cfg)
                when (val code = conn.responseCode) {
                    in 200..299 -> null
                    301, 302 -> null
                    401 -> ERR_AUTH_FAIL
                    403 -> ERR_NO_PERMISSION
                    404 -> null
                    405 -> null
                    else -> "$ERR_CONNECTION_FAILED:$code"
                }
            } finally { conn.disconnect() }
        } catch (_: Exception) { ERR_NETWORK_ERROR }
    }

    private fun httpConn(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 120000; c.useCaches = false
        c.doInput = true; c.instanceFollowRedirects = false
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        return c
    }

    private fun basicAuth(conn: HttpURLConnection, cfg: WebDavConfig) {
        val encoded = Base64.encodeToString("${cfg.username}:${cfg.password}".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        conn.setRequestProperty("Authorization", "Basic $encoded")
    }

    private fun fileUrl(cfg: WebDavConfig): String {
        val base = cfg.url.trimEnd('/')
        return try { URI("$base/songshushoupai_backup.zip").toASCIIString() }
               catch (_: Exception) { "$base/songshushoupai_backup.zip" }
    }

    private fun uploadZip(ctx: Context, cfg: WebDavConfig): String? {
        val tmp = File(ctx.cacheDir, "backup_${System.currentTimeMillis()}.zip")
        return try {
            val list = ImageDataManager.getImageList(ctx)
            FileOutputStream(tmp).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    zos.putNextEntry(ZipEntry("config.json"))
                    zos.write(buildConfigJson(ctx).toByteArray(Charsets.UTF_8)); zos.closeEntry()
                    writeImagesToZip(zos, ctx, list)
                }
            }
            val url = fileUrl(cfg)
            val conn = httpConn(url)
            try {
                conn.requestMethod = "PUT"; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/zip")
                conn.setRequestProperty("Content-Length", tmp.length().toString())
                basicAuth(conn, cfg)
                tmp.inputStream().use { it.copyTo(conn.outputStream) }
                when (val code = conn.responseCode) {
                    in 200..299 -> null
                    401 -> ERR_UPLOAD_AUTH_FAIL
                    403 -> ERR_UPLOAD_NO_PERMISSION
                    else -> "$ERR_UPLOAD_FAILED:$code"
                }
            } finally { conn.disconnect() }
        } catch (_: Exception) { ERR_UPLOAD_FAILED_SIMPLE }
        finally { tmp.delete() }
    }

    private fun downloadRestore(ctx: Context, cfg: WebDavConfig): String? {
        val tmp = File(ctx.cacheDir, "download_${System.currentTimeMillis()}.zip")
        return try {
            var url = fileUrl(cfg)
            var conn = httpConn(url)
            try {
                conn.requestMethod = "GET"; basicAuth(conn, cfg)
                var code = conn.responseCode
                val origHost = try { URI(url).host } catch (_: Exception) { "" }
                var redirects = 0
                while (code in listOf(301, 302, 307, 308) && redirects < 5) {
                    redirects++
                    val loc = conn.getHeaderField("Location") ?: return ERR_REDIRECT_NO_LOCATION
                    conn.disconnect()
                    url = if (loc.startsWith("http")) loc else "${cfg.url.trimEnd('/')}${if (loc.startsWith("/")) "" else "/"}$loc"
                    conn = httpConn(url); conn.requestMethod = "GET"
                    val rHost = try { URI(url).host } catch (_: Exception) { "" }
                    if (rHost.isNotEmpty() && rHost == origHost) basicAuth(conn, cfg)
                    else { conn.setRequestProperty("Referer", cfg.url.trimEnd('/')) }
                    code = conn.responseCode
                }
                if (code != 200) {
                    conn.disconnect()
                    return when (code) {
                        401 -> ERR_AUTH_FAIL
                        404 -> ERR_BACKUP_NOT_FOUND
                        403 -> ERR_DOWNLOAD_NO_PERMISSION
                        else -> "$ERR_SERVER_RESPONSE:$code"
                    }
                }
                tmp.outputStream().use { out -> conn.inputStream.use { it.copyTo(out) } }
            } finally { conn.disconnect() }

            if (tmp.length() == 0L) return ERR_DOWNLOADED_EMPTY
            saveToDownloads(ctx, tmp)

            val (configJson, imageMap, coverMap, qrMap) = readZipEntries(FileInputStream(tmp))
            if (configJson == null) return ERR_NO_CONFIG_IN_ZIP

            restoreFromEntries(ctx, configJson, imageMap, coverMap, qrMap, "restore")
            null
        } catch (_: Exception) { ERR_UNKNOWN_ERROR }
        finally { tmp.delete() }
    }

    private fun saveToDownloads(ctx: Context, src: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val ext = src.extension; val base = src.nameWithoutExtension
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "${base}_${System.currentTimeMillis()}.$ext")
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    ctx.contentResolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                    cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                    ctx.contentResolver.update(uri, cv, null, null)
                }
            } else {
                val dl = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
                if (dl.exists() || dl.mkdirs()) {
                    val dest = File(dl, src.name)
                    src.inputStream().use { ins -> dest.outputStream().use { out -> ins.copyTo(out) } }
                }
            }
        } catch (_: Exception) {}

    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
