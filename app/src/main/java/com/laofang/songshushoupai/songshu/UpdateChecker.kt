package com.laofang.songshushoupai.songshu

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val title: String, val description: String, val link: String, val pubDate: String)

object UpdateChecker {
    private const val FEED_URL = "https://songshushoupai.mysxl.cn/blog/feed.xml"

    private var checkedThisSession = false

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("rss_cache", Context.MODE_PRIVATE)

    private fun appVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }

    fun getCachedResult(ctx: Context): UpdateInfo? {
        val data = prefs(ctx).getString("cache_data", null) ?: return null
        if (data.isEmpty()) return null
        return try {
            val j = JSONObject(data)
            UpdateInfo(j.optString("version", ""), j.optString("title", ""), j.optString("description", ""), j.optString("link", ""), j.optString("pubDate", ""))
        } catch (_: Exception) { null }
    }

    private fun saveCache(ctx: Context, info: UpdateInfo?) {
        prefs(ctx).edit {
            putLong("cache_timestamp", System.currentTimeMillis())
            putString("cache_version", appVersion(ctx))
            putString("cache_data", if (info != null) JSONObject().apply {
                put("version", info.version); put("title", info.title); put("description", info.description)
                put("link", info.link); put("pubDate", info.pubDate)
            }.toString() else "")
        }
    }

    suspend fun checkForUpdate(ctx: Context, currentVersion: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        if (checkedThisSession) return@withContext Result.success(getCachedResult(ctx))
        try {
            val conn = URL(FEED_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Songshushoupai/1.0")
                if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
                val items = parseFeed(conn.inputStream.bufferedReader().readText())
                if (items.isEmpty()) { saveCache(ctx, null); checkedThisSession = true; return@withContext Result.success(null) }
                val latest = items.first()
                val ver = extractVersion(latest.title)
                val result = if (ver != null && isNewer(ver, currentVersion)) latest.copy(version = ver) else null
                saveCache(ctx, result)
                checkedThisSession = true
                Result.success(result)
            } finally { conn.disconnect() }
        } catch (e: Exception) {
            checkedThisSession = true
            getCachedResult(ctx)?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    private fun parseFeed(xml: String): List<UpdateInfo> {
        val out = mutableListOf<UpdateInfo>()
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            var tag = ""; var title = ""; var link = ""; var desc = ""; var date = ""; var inItem = false
            var evt = parser.eventType
            while (evt != XmlPullParser.END_DOCUMENT) {
                when (evt) {
                    XmlPullParser.START_TAG -> {
                        tag = parser.name
                        if (tag == "item") { inItem = true; title = ""; link = ""; desc = ""; date = "" }
                    }
                    XmlPullParser.TEXT -> if (inItem) when (tag) {
                        "title" -> title = parser.text; "link" -> link = parser.text
                        "description" -> desc = parser.text; "pubDate" -> date = parser.text
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            inItem = false
                            out.add(UpdateInfo("", title, desc.replace(Regex("<[^>]*>"), "").trim(), link, date))
                        }
                        tag = ""
                    }
                }
                evt = parser.next()
            }
        } catch (_: Exception) {}
        return out
    }

    private fun extractVersion(title: String) = Regex("""(\d+\.\d+\.\d+)""").find(title)?.groupValues?.get(1)

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }; val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true; if (lv < cv) return false
        }
        return false
    }
}
