package com.laofang.songshushoupai.songshu

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import android.os.Build
import androidx.compose.ui.tooling.preview.Preview
import com.laofang.songshushoupai.songshu.start.egg.ColorSudokuActivity

import com.laofang.songshushoupai.songshu.ui.theme.RedPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.YellowPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.TealPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.BluePrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.GreenPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.OrangePrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.PinkPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.PurplePrimaryLight
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

private val FoldEnter: EnterTransition = fadeIn(tween(250)) + expandVertically(tween(250))
private val FoldExit: ExitTransition = fadeOut(tween(200)) + shrinkVertically(tween(200))
private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun FoldableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorder(), CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "  ▲" else "  ▼",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded, enter = FoldEnter, exit = FoldExit) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = contentPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { content() }
            }
        }
    }
}

@Composable
private fun AboutLinkRow(text: String, url: String) {
    val ctx = LocalContext.current
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
    )
}

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsPage(
    scrollState: ScrollState = rememberScrollState(),
    onImportComplete: () -> Unit = {},
    basicConfigExpanded: Boolean = true,
    onBasicConfigExpandedChange: (Boolean) -> Unit = {},
    backupExpanded: Boolean = false,
    onBackupExpandedChange: (Boolean) -> Unit = {},
    aboutExpanded: Boolean = false,
    onAboutExpandedChange: (Boolean) -> Unit = {},
    themeExpanded: Boolean = false,
    onThemeExpandedChange: (Boolean) -> Unit = {},
    qrExpanded: Boolean = false,
    onQrExpandedChange: (Boolean) -> Unit = {},
    onThemeChanged: (Int) -> Unit = {},
    onDarkModeChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val saved = remember { SettingsManager.loadSettings(context) }

    var webdavUrl by remember { mutableStateOf("") }
    var webdavUser by remember { mutableStateOf("") }
    var webdavPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showBackupConfirmDialog by remember { mutableStateOf(false) }
    var backupConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showWebDavConfigDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf(UpdateChecker.getCachedResult(context)) }
    var iconClickCount by remember { mutableIntStateOf(0) }
    var defaultOrientation by remember { mutableStateOf(saved.defaultOrientation) }
    var keepScreenOn by remember { mutableStateOf(saved.keepScreenOn) }
    var showBattery by remember { mutableStateOf(saved.showBattery) }
    var lockOrientation by remember { mutableStateOf(saved.lockOrientation) }
    var antiBurnIn by remember { mutableStateOf(saved.antiBurnIn) }
    var muteVideo by remember { mutableStateOf(saved.muteVideo) }
    var currentDarkMode by remember { mutableIntStateOf(saved.darkMode) }
    var currentThemeColorIndex by remember { mutableIntStateOf(saved.themeColorIndex) }
    var customThemeColor by remember { mutableLongStateOf(saved.customThemeColor) }
    var showQrCode by remember { mutableStateOf(saved.showQrCode) }
    var qrCodePath by remember { mutableStateOf(saved.qrCodePath) }
    var qrPreviewBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val saveJob = remember { mutableListOf<Job?>(null) }

    LaunchedEffect(qrCodePath) {
        qrPreviewBmp = withContext(Dispatchers.IO) {
            val path = qrCodePath
            if (path.isNotEmpty() && File(path).exists()) {
                try { android.graphics.BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
            } else null
        }
    }

    fun saveAllSettings() {
        saveJob[0]?.cancel()
        saveJob[0] = scope.launch {
            delay(300.milliseconds)
            withContext(Dispatchers.IO) {
                SettingsManager.saveSettings(context, AppSettings(
                    defaultOrientation = defaultOrientation,
                    keepScreenOn = keepScreenOn,
                    showBattery = showBattery,
                    lockOrientation = lockOrientation,
                    antiBurnIn = antiBurnIn,
                    muteVideo = muteVideo,
                    themeColorIndex = currentThemeColorIndex,
                    customThemeColor = customThemeColor,
                    darkMode = currentDarkMode,
                    showQrCode = showQrCode,
                    qrCodePath = qrCodePath
                ))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            scope.launch {
                try {
                    val success = BackupManager.importFromZip(context, uri)
                    statusMessage = if (success) "导入成功" else "导入失败，文件格式不正确"
                    if (success) onImportComplete()
                } catch (e: Exception) { statusMessage = "导入失败: ${e.localizedMessage}" }
                isLoading = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isLoading = true
            scope.launch {
                try { BackupManager.exportToZip(context, uri); statusMessage = "导出成功" }
                catch (e: Exception) { statusMessage = "导出失败: ${e.localizedMessage}" }
                isLoading = false
            }
        }
    }

    val qrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it) ?: return@withContext
                        val dir = File(context.filesDir, "qrcodes").also { d -> d.mkdirs() }
                        val file = File(dir, "custom_qr.png")
                        FileOutputStream(file).use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        val path = file.absolutePath
                        qrCodePath = path
                        qrPreviewBmp = try {
                            android.graphics.BitmapFactory.decodeFile(path)
                        } catch (_: Throwable) { null }
                        saveAllSettings()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    if (statusMessage != null) {
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("提示") },
            text = { Text(statusMessage!!) },
            confirmButton = { TextButton(onClick = { statusMessage = null }) { Text("确定") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ===== 赞助卡片 =====
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(cardBorder(), CardShape)
                .clip(CardShape),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "支持开发者",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "V${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "欢迎通过以下渠道赞助",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.ifdian.net/a/laofang".toUri()))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("爱发电") }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://ko-fi.com/laofang".toUri()))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Ko-fi") }
                }
            }
        }

        // ===== 新版本提示 =====
        AnimatedVisibility(
            visible = updateInfo != null,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            updateInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(cardBorder(), CardShape)
                        .clip(CardShape)
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, info.link.toUri())) },
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "发现新版本 ${info.version}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "点击查看",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ===== 基本设置 =====
        FoldableCard("基本设置", basicConfigExpanded, onBasicConfigExpandedChange) {
            SettingsSwitchRow("反向显示", "打开此开关即为反向显示", defaultOrientation) {
                defaultOrientation = it; saveAllSettings()
            }
            HorizontalDivider()
            SettingsSwitchRow("常亮显示", "启动时屏幕保持常亮", keepScreenOn) {
                keepScreenOn = it; saveAllSettings()
            }
            HorizontalDivider()
            SettingsSwitchRow("显示电池电量", "在全屏播放时顶部显示电池电量", showBattery) {
                showBattery = it; saveAllSettings()
            }
            HorizontalDivider()
            SettingsSwitchRow("锁定方向", "锁定后长按不会旋转兽牌", lockOrientation) {
                lockOrientation = it; saveAllSettings()
            }
            HorizontalDivider()
            SettingsSwitchRow("防烧屏", "每隔5分钟轻微移动，防止屏幕残影", antiBurnIn) {
                antiBurnIn = it; saveAllSettings()
            }
            HorizontalDivider()
            SettingsSwitchRow("视频静音", "播放视频时默认静音", muteVideo) {
                muteVideo = it; saveAllSettings()
            }
        }

        // ===== 二维码设置 =====
        FoldableCard("二维码设置", qrExpanded, onQrExpandedChange) {
            SettingsSwitchRow("上划展示二维码", "打开上划屏幕展示二维码", showQrCode) {
                showQrCode = it; saveAllSettings()
            }
            HorizontalDivider()
            val qrAlpha = if (showQrCode) 1f else 0.4f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .graphicsLayer { alpha = qrAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "自定义二维码", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (qrCodePath.isNotEmpty()) "已导入自定义二维码" else "未导入，将使用默认示例图",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { qrImageLauncher.launch("image/*") },
                    enabled = showQrCode,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("导入") }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = qrAlpha },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrPreviewBmp != null) {
                        Image(
                            painter = BitmapPainter(qrPreviewBmp!!.asImageBitmap()),
                            contentDescription = "二维码预览",
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .heightIn(max = 200.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.qr_zanzhu),
                            contentDescription = "默认二维码",
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .heightIn(max = 200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // ===== 主题设置 =====
        FoldableCard("主题设置", themeExpanded, onThemeExpandedChange) {
            Text(
                text = "深色模式",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val darkModeOptions = remember {
                listOf(0 to "跟随系统", 1 to "浅色模式", 2 to "深色模式")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                darkModeOptions.forEach { (mode, label) ->
                    val isSelected = currentDarkMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable {
                                currentDarkMode = mode; saveAllSettings(); onDarkModeChanged(mode)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "主题色彩",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val themeOptions = remember {
                val isMonetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                buildList {
                    if (isMonetSupported) add(8 to "莫奈取色" to Color.Unspecified)
                    add(6 to "红色" to RedPrimaryLight)
                    add(2 to "橙色" to OrangePrimaryLight)
                    add(7 to "黄色" to YellowPrimaryLight)
                    add(1 to "绿色" to GreenPrimaryLight)
                    add(5 to "青色" to TealPrimaryLight)
                    add(0 to "蓝色" to BluePrimaryLight)
                    add(4 to "紫色" to PurplePrimaryLight)
                    add(3 to "粉色" to PinkPrimaryLight)
                }
            }
            themeOptions.forEach { (pair, previewColor) ->
                val (index, label) = pair
                val isSelected = currentThemeColorIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            currentThemeColorIndex = index; saveAllSettings(); onThemeChanged(index)
                        }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (index == 8) MaterialTheme.colorScheme.surfaceVariant else previewColor
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 8) {
                            Icon(
                                painter = painterResource(android.R.drawable.star_on),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ===== 数据备份 =====
        FoldableCard("数据备份", backupExpanded, onBackupExpandedChange) {
            Text(
                text = "本地备份",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "将配置和所有图片导出为ZIP文件，或从ZIP文件恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        showBackupConfirmDialog = true
                        backupConfirmAction = { exportLauncher.launch("songshushoupai_backup.zip") }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("导出配置") }
                OutlinedButton(
                    onClick = {
                        showBackupConfirmDialog = true
                        backupConfirmAction = { importLauncher.launch(arrayOf("application/zip")) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("导入配置") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "WebDAV 备份",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "配置WebDAV服务器地址，将备份上传到服务器或从服务器恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showWebDavConfigDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (webdavUrl.isNotBlank()) "服务器配置：已配置 (${webdavUrl.take(30)}...)"
                    else "点击配置 WebDAV 服务器"
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val config = WebDavConfig(webdavUrl, webdavUser, webdavPass)
                        if (config.url.isNotBlank()) {
                            showBackupConfirmDialog = true
                            backupConfirmAction = {
                                isLoading = true
                                scope.launch {
                                    val errorMsg = BackupManager.webdavUpload(context, config)
                                    statusMessage = if (errorMsg == null) "WebDAV上传成功" else "WebDAV上传失败: $errorMsg"
                                    isLoading = false
                                }
                            }
                        } else { statusMessage = "请先配置服务器地址" }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("备份到服务器")
                }
                OutlinedButton(
                    onClick = {
                        val config = WebDavConfig(webdavUrl, webdavUser, webdavPass)
                        if (config.url.isNotBlank()) {
                            showBackupConfirmDialog = true
                            backupConfirmAction = {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val errorMsg = BackupManager.webdavDownload(context, config)
                                        statusMessage = if (errorMsg == null) "恢复成功！备份文件已保存到「下载」目录" else "恢复失败: $errorMsg"
                                        if (errorMsg == null) onImportComplete()
                                    } catch (e: Exception) { statusMessage = "恢复出错: ${e.localizedMessage}" }
                                    isLoading = false
                                }
                            }
                        } else { statusMessage = "请先配置服务器地址" }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("从服务器恢复")
                }
            }
        }

        // ===== 关于软件 =====
        FoldableCard("关于软件", aboutExpanded, onAboutExpandedChange, contentPadding = 16.dp) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            iconClickCount++
                            if (iconClickCount >= 7) {
                                iconClickCount = 0
                                context.startActivity(Intent(context, ColorSudokuActivity::class.java))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(R.mipmap.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = "应用图标",
                        modifier = Modifier.size(108.dp).align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
                Text("松鼠兽牌", style = MaterialTheme.typography.titleMedium)
                Text("版本 V${BuildConfig.VERSION_NAME} ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "一款简单易用的兽牌制作工具，帮助你使用旧手机快速创建和展示自己的兽牌图片，适合需要兽牌但经费不足的小伙伴。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://qun.qq.com/universal-share/share?ac=1&authKey=EHQoZ%2BHU4s8taqGbkhUKrWY4FAq2vLB2LoPRNMO7jLkxGyyNLrnWCNmaZ7DuJTOx&busi_data=eyJncm91cENvZGUiOiI0NjUxNzQ2MTMiLCJ0b2tlbiI6IjlHTnZHTlhwMWVlN2VEYXJzTitoWkZxSlV1VUhadnNsYzJNVkE5b0tIcGZvQW00TGxOK0lQRGFMaEZjeU5GZWEiLCJ1aW4iOiIyOTA4ODA3NzYwIn0%3D&data=LITJR0_gfVVBcvQQSGd-RQZ7xQ39dz8b0w_wJzZ69Z-mCigq-1uJsrRMOstu12BG7-aKJJu0EhSDGJprd-kbKQ&svctype=4&tempid=h5_group_info".toUri()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("QQ群")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/LaoFang114514/songshu-phone-badge".toUri()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("GitHub")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://songshushoupai.mysxl.cn/".toUri()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("官网")
                }
            }
        }
    }

    // ===== 弹窗区域 =====
    if (showBackupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackupConfirmDialog = false; backupConfirmAction = null },
            title = { Text("确认操作") },
            text = { Text("此操作将覆盖当前数据，确定要继续吗？") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showBackupConfirmDialog = false; backupConfirmAction = null },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            showBackupConfirmDialog = false; backupConfirmAction?.invoke(); backupConfirmAction = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("确定") }
                }
            },
            dismissButton = {}
        )
    }

    if (showWebDavConfigDialog) {
        var dialogUrl by remember { mutableStateOf(webdavUrl) }
        var dialogUser by remember { mutableStateOf(webdavUser) }
        var dialogPass by remember { mutableStateOf(webdavPass) }
        var dialogTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showWebDavConfigDialog = false },
            title = { Text("WebDAV 服务器配置") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogUrl, onValueChange = { dialogUrl = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("https://example.com/remote.php/dav/files/user/") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !dialogTesting
                    )
                    OutlinedTextField(
                        value = dialogUser, onValueChange = { dialogUser = it },
                        label = { Text("用户名") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !dialogTesting
                    )
                    OutlinedTextField(
                        value = dialogPass, onValueChange = { dialogPass = it },
                        label = { Text("密码") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), enabled = !dialogTesting
                    )
                    if (dialogTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("测试连接中...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (dialogUrl.isBlank()) { statusMessage = "请先填写服务器地址"; return@OutlinedButton }
                            dialogTesting = true
                            scope.launch {
                                val config = WebDavConfig(dialogUrl, dialogUser, dialogPass)
                                val error = BackupManager.webdavTestConnection(config)
                                statusMessage = if (error == null) "连接成功!" else "连接失败: $error"
                                dialogTesting = false
                            }
                        },
                        enabled = !dialogTesting, shape = RoundedCornerShape(12.dp)
                    ) { Text("测试连接") }
                    Button(
                        onClick = {
                            webdavUrl = dialogUrl; webdavUser = dialogUser; webdavPass = dialogPass
                            BackupManager.saveWebDavConfig(context, WebDavConfig(dialogUrl, dialogUser, dialogPass))
                            showWebDavConfigDialog = false
                        },
                        enabled = !dialogTesting, shape = RoundedCornerShape(12.dp)
                    ) { Text("保存") }
                }
            },
            dismissButton = { TextButton(onClick = { showWebDavConfigDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
