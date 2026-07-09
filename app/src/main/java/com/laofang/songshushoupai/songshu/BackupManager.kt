package com.laofang.songshushoupai.songshu

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
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

data class WebDavConfig(val url: String = "", val username: String = "", val password: String = "")

object BackupManager {
    private const val TAG = "BackupManager"

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
            })
        }
        return JSONObject().apply { put("selected_index", sel); put("version", 2); put("images", arr) }.toString(2)
    }

    private fun writeImagesToZip(zos: ZipOutputStream, list: List<ImageItem>) {
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
    }

    fun exportToZip(ctx: Context, uri: Uri) {
        val list = ImageDataManager.getImageList(ctx)
        ctx.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                zos.putNextEntry(ZipEntry("config.json"))
                zos.write(buildConfigJson(ctx).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
                writeImagesToZip(zos, list)
            }
        }
    }

    private fun readZipEntries(input: java.io.InputStream): Triple<String?, MutableMap<String, ByteArray>, MutableMap<String, ByteArray>> {
        var configJson: String? = null
        val imageMap = mutableMapOf<String, ByteArray>()
        val coverMap = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val data = zis.readBytes()
                    when {
                        entry.name == "config.json" -> configJson = String(data, Charsets.UTF_8)
                        entry.name.startsWith("images/") -> imageMap[entry.name] = data
                        entry.name.startsWith("covers/") -> coverMap[entry.name] = data
                    }
                }
                zis.closeEntry(); entry = zis.nextEntry
            }
        }
        return Triple(configJson, imageMap, coverMap)
    }

    private fun restoreFromEntries(ctx: Context, configJson: String, imageMap: Map<String, ByteArray>, coverMap: Map<String, ByteArray>): Boolean {
        val cfg = JSONObject(configJson)
        val arr = cfg.getJSONArray("images")
        val sel = cfg.optInt("selected_index", 0)
        val imgDir = File(ctx.filesDir, "images").also { it.mkdirs() }
        val covDir = File(ctx.filesDir, "covers").also { it.mkdirs() }
        val ts = System.currentTimeMillis()
        val newList = mutableListOf<ImageItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("name", "兽牌 ${i + 1}")
            val oldPath = o.optString("path", "")
            val type = o.optString("type", "image")
            val oldCover = o.optString("cover", "")
            var newPath = ""
            if (oldPath.isNotEmpty()) {
                val fn = File(oldPath).name
                val key = imageMap.keys.find { it.endsWith(fn) }
                if (key != null) { val f = File(imgDir, "import_${ts}_${i}_$fn"); f.writeBytes(imageMap[key]!!); newPath = f.absolutePath }
            }
            var newCoverPath = ""
            if (oldCover.isNotEmpty()) {
                val fn = File(oldCover).name
                val key = coverMap.keys.find { it.endsWith(fn) }
                if (key != null) { val f = File(covDir, "import_${ts}_${i}_$fn"); f.writeBytes(coverMap[key]!!); newCoverPath = f.absolutePath }
            }
            newList.add(ImageItem(i, newPath, name, type, newCoverPath))
        }
        saveImageList(ctx, newList, sel)
        return true
    }

    fun importFromZip(ctx: Context, uri: Uri): Boolean {
        return try {
            val (configJson, imageMap, coverMap) = ctx.contentResolver.openInputStream(uri)?.use { readZipEntries(
                it) }
                ?: return false
            if (configJson == null) return false
            restoreFromEntries(ctx, configJson, imageMap, coverMap)
        } catch (e: Exception) { Log.e(TAG, "Import failed", e); false }
    }

    suspend fun webdavUpload(ctx: Context, cfg: WebDavConfig): String? = withContext(Dispatchers.IO) { uploadZip(ctx, cfg) }
    suspend fun webdavDownload(ctx: Context, cfg: WebDavConfig): String? = withContext(Dispatchers.IO) { downloadRestore(ctx, cfg) }

    suspend fun webdavTestConnection(cfg: WebDavConfig): String? = withContext(Dispatchers.IO) {
        try {
            val conn = httpConn(fileUrl(cfg))
            try {
                conn.requestMethod = "HEAD"
                basicAuth(conn, cfg)
                when (val code = conn.responseCode) {
                    in 200..299 -> null
                    301, 302 -> null
                    401 -> "认证失败，请检查用户名和密码"
                    403 -> "无权限访问 (403)，请检查服务器地址是否包含用户名路径"
                    404 -> null
                    405 -> null
                    else -> "连接失败: $code ${conn.responseMessage}"
                }
            } finally { conn.disconnect() }
        } catch (e: Exception) { e.localizedMessage ?: "连接失败，请检查网络" }
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
                    writeImagesToZip(zos, list)
                }
            }
            val url = fileUrl(cfg)
            Log.d(TAG, "Upload to: $url, size=${tmp.length()}")
            val conn = httpConn(url)
            try {
                conn.requestMethod = "PUT"; conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/zip")
                conn.setRequestProperty("Content-Length", tmp.length().toString())
                basicAuth(conn, cfg)
                tmp.inputStream().use { it.copyTo(conn.outputStream) }
                when (val code = conn.responseCode) {
                    in 200..299 -> null
                    401 -> "上传认证失败，请检查用户名和密码"
                    403 -> "上传无权限 (403)"
                    else -> "上传失败: $code ${conn.responseMessage}"
                }
            } finally { conn.disconnect() }
        } catch (e: Exception) { Log.e(TAG, "Upload failed", e); e.localizedMessage ?: "上传失败" }
        finally { tmp.delete() }
    }

    private fun downloadRestore(ctx: Context, cfg: WebDavConfig): String? {
        val tmp = File(ctx.cacheDir, "download_${System.currentTimeMillis()}.zip")
        return try {
            var url = fileUrl(cfg)
            Log.d(TAG, "Download from: $url")
            var conn = httpConn(url)
            try {
                conn.requestMethod = "GET"; basicAuth(conn, cfg)
                var code = conn.responseCode; var msg = conn.responseMessage
                Log.d(TAG, "Download response: $code $msg")
                val origHost = try { URI(url).host } catch (_: Exception) { "" }
                var redirects = 0
                while (code in listOf(301, 302, 307, 308) && redirects < 5) {
                    redirects++
                    val loc = conn.getHeaderField("Location") ?: return "服务器重定向但未提供 Location 头"
                    conn.disconnect()
                    url = if (loc.startsWith("http")) loc else "${cfg.url.trimEnd('/')}${if (loc.startsWith("/")) "" else "/"}$loc"
                    Log.d(TAG, "Redirect ($redirects) to: $url")
                    conn = httpConn(url); conn.requestMethod = "GET"
                    val rHost = try { URI(url).host } catch (_: Exception) { "" }
                    if (rHost.isNotEmpty() && rHost == origHost) basicAuth(conn, cfg)
                    else { Log.d(TAG, "Redirect different host, skipping auth"); conn.setRequestProperty("Referer", cfg.url.trimEnd('/')) }
                    code = conn.responseCode; msg = conn.responseMessage
                }
                if (code != 200) {
                    conn.disconnect()
                    return when (code) {
                        401 -> "认证失败，请检查用户名和密码"
                        404 -> "服务器上未找到备份文件，请先上传"
                        403 -> "无权限下载 (403)"
                        else -> "服务器返回 $code $msg"
                    }
                }
                tmp.outputStream().use { out -> conn.inputStream.use { it.copyTo(out) } }
            } finally { conn.disconnect() }

            if (tmp.length() == 0L) return "下载的文件为空"
            saveToDownloads(ctx, tmp)

            val (configJson, imageMap, coverMap) = readZipEntries(FileInputStream(tmp))
            if (configJson == null) return "ZIP文件中未找到配置文件 config.json"

            val cfg2 = JSONObject(configJson)
            val arr = cfg2.getJSONArray("images")
            val sel = cfg2.optInt("selected_index", 0)
            val imgDir = File(ctx.filesDir, "images").also { it.mkdirs() }
            val covDir = File(ctx.filesDir, "covers").also { it.mkdirs() }
            val ts = System.currentTimeMillis()
            val newList = mutableListOf<ImageItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "兽牌 ${i + 1}")
                val oldPath = o.optString("path", ""); val type = o.optString("type", "image"); val oldCover = o.optString("cover", "")
                var np = ""; var ncp = ""
                if (oldPath.isNotEmpty()) {
                    val fn = File(oldPath).name; val key = imageMap.keys.find { it.endsWith(fn) }
                    if (key != null) { val f = File(imgDir, "restore_${ts}_${i}_$fn"); f.writeBytes(imageMap[key]!!); np = f.absolutePath }
                }
                if (oldCover.isNotEmpty()) {
                    val fn = File(oldCover).name; val key = coverMap.keys.find { it.endsWith(fn) }
                    if (key != null) { val f = File(covDir, "restore_${ts}_${i}_$fn"); f.writeBytes(coverMap[key]!!); ncp = f.absolutePath }
                }
                newList.add(ImageItem(i, np, name, type, ncp))
            }
            saveImageList(ctx, newList, sel)
            Log.d(TAG, "Restore OK: ${newList.size} items")
            null
        } catch (e: Exception) { Log.e(TAG, "Download/Restore failed", e); e.localizedMessage ?: "未知错误" }
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
                val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dl.exists() || dl.mkdirs()) {
                    val dest = File(dl, src.name)
                    src.inputStream().use { ins -> dest.outputStream().use { out -> ins.copyTo(out) } }
                }
            }
        } catch (e: Exception) { Log.w(TAG, "Save to Downloads failed (non-fatal)", e) }
    }

    private fun saveImageList(ctx: Context, list: List<ImageItem>, sel: Int) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().apply { put("path", it.filePath); put("name", it.name); put("type", it.type); put("cover", it.coverPath) }) }
        ctx.getSharedPreferences("image_data", Context.MODE_PRIVATE).edit {
            putString("image_list", arr.toString()); putInt("selected_index", sel)
        }
    }
}
