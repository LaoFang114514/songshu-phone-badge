package com.laofang.songshushoupai.songshu

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AppSettings(
    val defaultOrientation: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showBattery: Boolean = false,
    val lockOrientation: Boolean = false,
    val antiBurnIn: Boolean = false,
    val muteVideo: Boolean = false,
    val themeColorIndex: Int = 0,
    val customThemeColor: Long = 0xFF1E88E5,
    val darkMode: Int = 0,
    val showQrCode: Boolean = false,
    val qrCodePath: String = ""
)

object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(context: Context): AppSettings {
        val p = prefs(context)
        // 首次启动初始化主题色
        if (!p.getBoolean("has_initialized_theme", false)) {
            val defaultIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 8 else 0
            p.edit {
                putInt("theme_color_index", defaultIndex)
                putLong("custom_theme_color", 0xFF1E88E5)
                putInt("dark_mode", 0)
                putBoolean("has_initialized_theme", true)
            }
            return AppSettings(themeColorIndex = defaultIndex)
        }
        return AppSettings(
            defaultOrientation = p.getBoolean("default_orientation", false),
            keepScreenOn = p.getBoolean("keep_screen_on", false),
            showBattery = p.getBoolean("show_battery", false),
            lockOrientation = p.getBoolean("lock_orientation", false),
            antiBurnIn = p.getBoolean("anti_burn_in", false),
            muteVideo = p.getBoolean("mute_video", false),
            themeColorIndex = p.getInt("theme_color_index", 0),
            customThemeColor = p.getLong("custom_theme_color", 0xFF1E88E5),
            darkMode = p.getInt("dark_mode", 0),
            showQrCode = p.getBoolean("show_qr_code", false),
            qrCodePath = p.getString("qr_code_path", "") ?: ""
        )
    }

    fun saveSettings(context: Context, s: AppSettings) {
        prefs(context).edit {
            putBoolean("default_orientation", s.defaultOrientation)
            putBoolean("keep_screen_on", s.keepScreenOn)
            putBoolean("show_battery", s.showBattery)
            putBoolean("lock_orientation", s.lockOrientation)
            putBoolean("anti_burn_in", s.antiBurnIn)
            putBoolean("mute_video", s.muteVideo)
            putInt("theme_color_index", s.themeColorIndex)
            putLong("custom_theme_color", s.customThemeColor)
            putInt("dark_mode", s.darkMode)
            putBoolean("show_qr_code", s.showQrCode)
            putString("qr_code_path", s.qrCodePath)
        }
    }
}

object ImageDataManager {
    private const val PREFS_NAME = "image_data"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getImageList(context: Context): List<ImageItem> {
        val raw = prefs(context).getString("image_list", null) ?: return defaultList()
        if (raw.isEmpty()) return defaultList()

        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ImageItem(
                    index = i,
                    filePath = obj.optString("path", ""),
                    name = obj.optString("name", "兽牌 ${i + 1}"),
                    type = obj.optString("type", "image"),
                    coverPath = obj.optString("cover", "")
                )
            }
        } catch (_: Exception) {
            // 兼容旧版 "|" 分隔符格式
            raw.split("|").mapIndexed { i, path ->
                ImageItem(i, path, "兽牌 ${i + 1}", "image")
            }
        }
    }

    private fun defaultList() = listOf(ImageItem(0, "", "默认兽牌"))

    fun getSelectedIndex(context: Context) = prefs(context).getInt("selected_index", 0)

    fun setSelectedIndex(context: Context, index: Int) {
        prefs(context).edit { putInt("selected_index", index) }
    }

    fun getCurrentImagePath(context: Context): String {
        val list = getImageList(context)
        val idx = getSelectedIndex(context).coerceIn(0, (list.size - 1).coerceAtLeast(0))
        return list.getOrNull(idx)?.filePath ?: ""
    }

    fun addImageToList(context: Context, filePath: String) {
        addItem(context, ImageItem(0, filePath, "", "image"))
    }

    fun addVideoToList(context: Context, filePath: String, coverPath: String = "") {
        addItem(context, ImageItem(0, filePath, "", "video", coverPath))
    }

    private fun addItem(context: Context, item: ImageItem) {
        val list = getImageList(context).toMutableList()
        val name = "兽牌 ${if (list.size == 1 && list[0].filePath.isEmpty()) 1 else list.size + 1}"
        val finalItem = item.copy(name = name)
        if (list.size == 1 && list[0].filePath.isEmpty()) list[0] = finalItem.copy(index = 0)
        else list.add(finalItem.copy(index = list.size))
        saveList(context, list)
    }

    fun deleteImage(context: Context, index: Int) {
        val list = getImageList(context).toMutableList()
        if (index !in list.indices) return
        val item = list[index]
        if (item.filePath.isNotEmpty()) File(item.filePath).delete()
        if (item.coverPath.isNotEmpty()) File(item.coverPath).delete()
        list.removeAt(index)
        if (list.isEmpty()) list.add(ImageItem(0, "", "默认兽牌"))
        reindex(list)
        saveList(context, list)

        val selected = getSelectedIndex(context)
        when {
            selected == index -> setSelectedIndex(context, 0)
            selected > index -> setSelectedIndex(context, selected - 1)
        }
    }

    fun moveItem(context: Context, from: Int, to: Int) {
        val list = getImageList(context).toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val selected = getSelectedIndex(context)
        val item = list.removeAt(from)
        list.add(to, item)
        reindex(list)
        saveList(context, list)
        when (selected) {
            from -> setSelectedIndex(context, to)
            in (from + 1)..to -> setSelectedIndex(context, selected - 1)
            in to..<from -> setSelectedIndex(context, selected + 1)
        }
    }

    fun replaceImage(context: Context, index: Int, newFilePath: String) {
        val list = getImageList(context).toMutableList()
        if (index !in list.indices) return
        val old = list[index]
        if (old.filePath.isNotEmpty()) File(old.filePath).delete()
        if (old.coverPath.isNotEmpty()) File(old.coverPath).delete()
        list[index] = ImageItem(index, newFilePath, old.name)
        saveList(context, list)
    }

    fun renameItem(context: Context, index: Int, newName: String) {
        val list = getImageList(context).toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].copy(name = newName)
        saveList(context, list)
    }

    fun restoreList(context: Context, list: List<ImageItem>, selectedIndex: Int) {
        saveList(context, list)
        setSelectedIndex(context, selectedIndex)
    }

    private fun reindex(list: MutableList<ImageItem>) {
        for (i in list.indices) list[i] = list[i].copy(index = i)
    }

    private fun saveList(context: Context, list: List<ImageItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("path", it.filePath)
                put("name", it.name)
                put("type", it.type)
                put("cover", it.coverPath)
            })
        }
        prefs(context).edit { putString("image_list", arr.toString()) }
    }
}

data class ImageItem(
    val index: Int,
    val filePath: String,
    val name: String,
    val type: String = "image",
    val coverPath: String = ""
) {
    val isVideo: Boolean get() = type == "video"
}