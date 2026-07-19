package com.laofang.songshushoupai.songshu

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.laofang.songshushoupai.songshu.settings.BackupOperation
import com.laofang.songshushoupai.songshu.settings.BasicSettingsCard
import com.laofang.songshushoupai.songshu.settings.QrCodeSettingsCard
import com.laofang.songshushoupai.songshu.settings.ThemeSettingsCard
import com.laofang.songshushoupai.songshu.settings.BackupSettingsCard
import com.laofang.songshushoupai.songshu.settings.AboutSettingsCard
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val CardShape = RoundedCornerShape(16.dp)
private val BtnShape = RoundedCornerShape(12.dp)

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingsPageScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) { content() }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape).clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(" ▶", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提示") },
        text = { Text(message) },
        confirmButton = { Button(onClick = onDismiss, shape = BtnShape) { Text("确定") } },
        shape = BtnShape
    )
}

@Composable
private fun BackupConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认操作") },
        text = { Text("此操作将覆盖当前数据，确定要继续吗？") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = BtnShape) { Text("取消") }
                Button(onClick = onConfirm, shape = BtnShape) { Text("确定") }
            }
        },
        dismissButton = {},
        shape = BtnShape
    )
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

@Composable
fun SettingsPage(
    onNavigateToBasicSettings: () -> Unit = {},
    onNavigateToQrCodeSettings: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToBackupSettings: () -> Unit = {},
    onNavigateToAboutSettings: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf(UpdateChecker.getCachedResult(ctx)) }
    var checkingUpdate by remember { mutableStateOf(false) }

    fun doCheckUpdate(forceRefresh: Boolean) {
        if (checkingUpdate) return
        checkingUpdate = true
        scope.launch {
            if (forceRefresh) {
                ctx.getSharedPreferences("rss_cache", Context.MODE_PRIVATE).edit { putString("cache_data", "") }
            }
            val result = UpdateChecker.checkForUpdate(ctx, BuildConfig.VERSION_NAME)
            result.getOrNull()?.let { updateInfo = it }
            checkingUpdate = false
        }
    }

    LaunchedEffect(Unit) { doCheckUpdate(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("支持开发者", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { doCheckUpdate(true) }
                    ) {
                        Text(
                            if (checkingUpdate) "检查中..." else "V${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("欢迎通过以下渠道赞助", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { openUrl(ctx, "https://www.ifdian.net/a/laofang") }, Modifier.weight(1f), shape = BtnShape) { Text("爱发电") }
                    OutlinedButton(onClick = { openUrl(ctx, "https://ko-fi.com/laofang") }, Modifier.weight(1f), shape = BtnShape) { Text("Ko-fi") }
                }
            }
        }

        updateInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape)
                    .clickable { openUrl(ctx, info.link) }
                    .clip(CardShape),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("发现新版本 ${info.version}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                        Text("点击查看", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        var emergencyExpanded by remember { mutableStateOf(true) }
        Card(
            modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { emergencyExpanded = !emergencyExpanded }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("这是一条临时通知", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(if (emergencyExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(
                    visible = emergencyExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        Image(
                            painterResource(R.drawable.image_1784470341594_62),
                            "这是一条临时通知",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "大家好，我是呆龙，塔塔的cp，塔塔在得闲兽聚突发中风，目前的情况很危险，很多朋友在帮我们我非常感激各位，我知道他的性格不是很讨喜，可能很多人讨厌他，但是他的家庭并不是很富裕已经无力扶起高昂的医疗费用。\n" +
                            "我记得第一次跟他参加展会，第一次一起在武汉聚会，我没法漠视他这样离开。\n" +
                            "我作为他的对象如果放弃的话他会失去生命，如果病情继续恶化我会无力负担高额的治疗费用，但是我不愿意放弃他的生命，恳请各位提供力所能及的帮助，不胜感激。\n" +
                            "量力而行，恳请各位转发谢谢",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        NavRow("基本设置") { onNavigateToBasicSettings() }
        NavRow("二维码设置") { onNavigateToQrCodeSettings() }
        NavRow("主题设置") { onNavigateToThemeSettings() }
        NavRow("数据备份") { onNavigateToBackupSettings() }
        NavRow("关于软件") { onNavigateToAboutSettings() }
    }
}

@Composable
fun AboutSettingsPage() {
    SettingsPageScaffold { AboutSettingsCard() }
}

@Composable
fun BasicSettingsPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var defaultOrientation by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(false) }
    var lockOrientation by remember { mutableStateOf(false) }
    var antiBurnIn by remember { mutableStateOf(false) }
    var muteVideo by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf<AppSettings?>(null) }

    LaunchedEffect(Unit) {
        SettingsManager.loadSettings(ctx).also { s ->
            base = s; defaultOrientation = s.defaultOrientation; keepScreenOn = s.keepScreenOn
            showBattery = s.showBattery; lockOrientation = s.lockOrientation
            antiBurnIn = s.antiBurnIn; muteVideo = s.muteVideo
        }
    }

    fun save() {
        val b = base ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                SettingsManager.saveSettings(ctx, b.copy(
                    defaultOrientation = defaultOrientation, keepScreenOn = keepScreenOn,
                    showBattery = showBattery, lockOrientation = lockOrientation,
                    antiBurnIn = antiBurnIn, muteVideo = muteVideo
                ))
            }
        }
    }

    SettingsPageScaffold {
        BasicSettingsCard(
            defaultOrientation = defaultOrientation, onDefaultOrientationChange = { defaultOrientation = it; save() },
            keepScreenOn = keepScreenOn, onKeepScreenOnChange = { keepScreenOn = it; save() },
            showBattery = showBattery, onShowBatteryChange = { showBattery = it; save() },
            lockOrientation = lockOrientation, onLockOrientationChange = { lockOrientation = it; save() },
            antiBurnIn = antiBurnIn, onAntiBurnInChange = { antiBurnIn = it; save() },
            muteVideo = muteVideo, onMuteVideoChange = { muteVideo = it; save() }
        )
    }
}

@Composable
fun QrCodeSettingsPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQrCode by remember { mutableStateOf(false) }
    var qrCodePath by remember { mutableStateOf("") }
    var qrPreviewBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var base by remember { mutableStateOf<AppSettings?>(null) }

    LaunchedEffect(Unit) {
        SettingsManager.loadSettings(ctx).also { s ->
            base = s; showQrCode = s.showQrCode; qrCodePath = s.qrCodePath
        }
    }
    LaunchedEffect(qrCodePath) {
        qrPreviewBmp = withContext(Dispatchers.IO) {
            if (qrCodePath.isNotEmpty() && File(qrCodePath).exists())
                try { android.graphics.BitmapFactory.decodeFile(qrCodePath) } catch (_: Throwable) { null }
            else null
        }
    }

    fun save() {
        val b = base ?: return
        scope.launch { withContext(Dispatchers.IO) { SettingsManager.saveSettings(ctx, b.copy(showQrCode = showQrCode, qrCodePath = qrCodePath)) } }
    }

    SettingsPageScaffold {
        QrCodeSettingsCard(
            showQrCode = showQrCode, onShowQrCodeChange = { showQrCode = it; save() },
            qrCodePath = qrCodePath, onQrCodePathChange = { qrCodePath = it; save() },
            qrPreviewBmp = qrPreviewBmp, onQrPreviewBmpChange = { qrPreviewBmp = it }
        )
    }
}

@Composable
fun ThemeSettingsPage(onThemeChanged: (Int) -> Unit, onDarkModeChanged: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var darkMode by remember { mutableIntStateOf(0) }
    var themeColor by remember { mutableIntStateOf(0) }
    var base by remember { mutableStateOf<AppSettings?>(null) }

    LaunchedEffect(Unit) {
        SettingsManager.loadSettings(ctx).also { s -> base = s; darkMode = s.darkMode; themeColor = s.themeColorIndex }
    }

    fun save() {
        val b = base ?: return
        scope.launch { withContext(Dispatchers.IO) { SettingsManager.saveSettings(ctx, b.copy(darkMode = darkMode, themeColorIndex = themeColor)) } }
    }

    SettingsPageScaffold {
        ThemeSettingsCard(
            currentDarkMode = darkMode,
            onDarkModeChange = { darkMode = it; save(); onDarkModeChanged(it) },
            currentThemeColorIndex = themeColor,
            onThemeColorIndexChange = { themeColor = it; save(); onThemeChanged(it) }
        )
    }
}

@Composable
fun BackupSettingsPage(onDataChanged: () -> Unit = {}) {
    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var pendingOp by remember { mutableStateOf<BackupOperation?>(null) }
    var webdavUrl by remember { mutableStateOf("") }
    var webdavUser by remember { mutableStateOf("") }
    var webdavPass by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var savedUrl by remember { mutableStateOf("") }
    var savedUser by remember { mutableStateOf("") }
    var savedPass by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        BackupManager.loadWebDavConfig(ctx).also { c ->
            webdavUrl = c.url; webdavUser = c.username; webdavPass = c.password
            savedUrl = c.url; savedUser = c.username; savedPass = c.password
        }
    }

    val isConfigModified = webdavUrl != savedUrl || webdavUser != savedUser || webdavPass != savedPass

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isLoading = true
        scope.launch {
            val msg = withContext(Dispatchers.IO) {
                try { BackupManager.exportToZip(appCtx, uri); "导出成功" } catch (e: Exception) { "导出失败: ${e.localizedMessage}" }
            }
            statusMsg = msg; isLoading = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isLoading = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { BackupManager.importFromZip(appCtx, uri) }
            statusMsg = if (ok) "导入成功" else "导入失败"; isLoading = false
            if (ok) onDataChanged()
        }
    }

    fun exec(op: BackupOperation) {
        when (op) {
            BackupOperation.EXPORT -> exportLauncher.launch("songshushoupai_backup.zip")
            BackupOperation.IMPORT -> importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            BackupOperation.WEBDAV_UPLOAD, BackupOperation.WEBDAV_DOWNLOAD -> {
                val cfg = BackupManager.loadWebDavConfig(appCtx)
                if (cfg.url.isBlank()) { statusMsg = "请先配置服务器地址"; return }
                isLoading = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        if (op == BackupOperation.WEBDAV_UPLOAD) BackupManager.webdavUpload(appCtx, cfg)
                        else BackupManager.webdavDownload(appCtx, cfg)
                    }
                    statusMsg = result ?: if (op == BackupOperation.WEBDAV_UPLOAD) "备份到服务器成功" else "从服务器恢复成功"
                    isLoading = false
                    if (result == null && op == BackupOperation.WEBDAV_DOWNLOAD) onDataChanged()
                }
            }
        }
    }

    statusMsg?.let { StatusDialog(message = it, onDismiss = { statusMsg = null }) }

    SettingsPageScaffold {
        BackupSettingsCard(
            context = ctx,
            isLoading = isLoading,
            onBackupOperation = { pendingOp = it; showConfirm = true },
            webdavUrl = webdavUrl, webdavUser = webdavUser, webdavPass = webdavPass,
            onWebdavUrlChange = { webdavUrl = it },
            onWebdavUserChange = { webdavUser = it },
            onWebdavPassChange = { webdavPass = it },
            onTestConnection = {
                if (webdavUrl.isBlank()) { statusMsg = "请先填写服务器地址"; return@BackupSettingsCard }
                isTesting = true
                scope.launch {
                    val err = BackupManager.webdavTestConnection(WebDavConfig(webdavUrl, webdavUser, webdavPass))
                    statusMsg = if (err == null) "连接成功!" else "连接失败: $err"
                    isTesting = false
                }
            },
            onSaveWebDavConfig = {
                BackupManager.saveWebDavConfig(appCtx, WebDavConfig(webdavUrl, webdavUser, webdavPass))
                savedUrl = webdavUrl; savedUser = webdavUser; savedPass = webdavPass
                statusMsg = "WebDAV 配置已保存"
            },
            isTesting = isTesting,
            isConfigModified = isConfigModified
        )
    }

    if (showConfirm) {
        BackupConfirmDialog(
            onDismiss = { showConfirm = false; pendingOp = null },
            onConfirm = { showConfirm = false; pendingOp?.let { exec(it) }; pendingOp = null }
        )
    }
}
