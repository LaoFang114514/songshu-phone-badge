package com.laofang.songshushoupai.songshu

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.laofang.songshushoupai.songshu.settings.BasicSettingsCard
import com.laofang.songshushoupai.songshu.settings.QrCodeSettingsCard
import com.laofang.songshushoupai.songshu.settings.ThemeSettingsCard
import com.laofang.songshushoupai.songshu.settings.BackupSettingsCard
import com.laofang.songshushoupai.songshu.start.egg.ColorSudokuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingsPageScaffold(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) { content() }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(cardBorder(), CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = " ▶",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提示") },
        text = { Text(message) },
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("确定") } }
    )
}

@Composable
private fun BackupConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认操作") },
        text = { Text("此操作将覆盖当前数据，确定要继续吗？") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("取消") }
                Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) { Text("确定") }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun WebDavConfigDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (WebDavConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogUrl by remember { mutableStateOf(initialUrl) }
    var dialogUser by remember { mutableStateOf("") }
    var dialogPass by remember { mutableStateOf("") }
    var dialogTesting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    if (statusMessage != null) {
        StatusDialog(message = statusMessage!!, onDismiss = { statusMessage = null })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        dialogTesting = false
                        onSave(WebDavConfig(dialogUrl, dialogUser, dialogPass))
                        BackupManager.saveWebDavConfig(context, WebDavConfig(dialogUrl, dialogUser, dialogPass))
                        onDismiss()
                    },
                    enabled = !dialogTesting, shape = RoundedCornerShape(12.dp)
                ) { Text("保存") }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("取消") } }
    )
}

@Composable
fun SettingsPage(
    onNavigateToBasicSettings: () -> Unit = {},
    onNavigateToQrCodeSettings: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    onNavigateToBackupSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAboutPopup by remember { mutableStateOf(false) }
    val updateInfo by remember { mutableStateOf(UpdateChecker.getCachedResult(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape),
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.ifdian.net/a/laofang".toUri())) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("爱发电") }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://ko-fi.com/laofang".toUri())) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Ko-fi") }
                }
            }
        }

        if (updateInfo != null) {
            val info = updateInfo!!
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
                    Text(
                        text = "发现新版本 ${info.version}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        NavRow("基本设置") { onNavigateToBasicSettings() }
        NavRow("二维码设置") { onNavigateToQrCodeSettings() }
        NavRow("主题设置") { onNavigateToThemeSettings() }
        NavRow("数据备份") { onNavigateToBackupSettings() }
        NavRow("关于软件") { showAboutPopup = true }
    }

    if (showAboutPopup) {
        var iconClickCount by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showAboutPopup = false },
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                iconClickCount++
                                if (iconClickCount >= 7) {
                                    iconClickCount = 0
                                    try {
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            val intent = Intent(activity, ColorSudokuActivity::class.java)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } else {
                                            android.widget.Toast.makeText(context, "无法访问页面", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "跳转失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(R.mipmap.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
                        Image(painter = painterResource(R.mipmap.ic_launcher_foreground), contentDescription = "应用图标", modifier = Modifier.size(98.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("松鼠兽牌", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Text(
                            text = "V${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "一款简单易用的旧手机变兽牌工具，帮助你使用旧手机快速创建和展示自己的兽牌图片，适合需要兽牌但经费不足的小伙伴。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://qun.qq.com/universal-pop/pop.html?ucid=JxRTxJmIQfa8p4d0_U_TyZyEn&gc=&sign=dc86ae0ca4700c5dbc23894f0fdb82fccfd937cd0fe5edec4afa9472c7300d07&external=&_type=gp&o&_client=yqq&hash=-".toUri())) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("QQ群") }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/LaoFang114514/songshu-phone-badge".toUri())) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("GitHub") }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://songshushoupai.mysxl.cn/".toUri())) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("官网") }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
fun BasicSettingsPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var defaultOrientation by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(false) }
    var lockOrientation by remember { mutableStateOf(false) }
    var antiBurnIn by remember { mutableStateOf(false) }
    var muteVideo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val s = SettingsManager.loadSettings(context)
        defaultOrientation = s.defaultOrientation
        keepScreenOn = s.keepScreenOn
        showBattery = s.showBattery
        lockOrientation = s.lockOrientation
        antiBurnIn = s.antiBurnIn
        muteVideo = s.muteVideo
    }

    fun save() {
        scope.launch {
            withContext(Dispatchers.IO) {
                SettingsManager.saveSettings(context, AppSettings(
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQrCode by remember { mutableStateOf(false) }
    var qrCodePath by remember { mutableStateOf("") }
    var qrPreviewBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var baseSettings by remember { mutableStateOf<AppSettings?>(null) }

    LaunchedEffect(Unit) {
        val s = SettingsManager.loadSettings(context)
        baseSettings = s
        showQrCode = s.showQrCode
        qrCodePath = s.qrCodePath
    }

    LaunchedEffect(qrCodePath) {
        qrPreviewBmp = withContext(Dispatchers.IO) {
            if (qrCodePath.isNotEmpty() && File(qrCodePath).exists()) {
                try { android.graphics.BitmapFactory.decodeFile(qrCodePath) } catch (_: Throwable) { null }
            } else null
        }
    }

    fun save() {
        val base = baseSettings ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                SettingsManager.saveSettings(context, base.copy(
                    showQrCode = showQrCode,
                    qrCodePath = qrCodePath
                ))
            }
        }
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
fun ThemeSettingsPage(
    onThemeChanged: (Int) -> Unit,
    onDarkModeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentDarkMode by remember { mutableIntStateOf(0) }
    var currentThemeColorIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val s = SettingsManager.loadSettings(context)
        currentDarkMode = s.darkMode
        currentThemeColorIndex = s.themeColorIndex
    }

    fun save() {
        scope.launch {
            withContext(Dispatchers.IO) {
                SettingsManager.saveSettings(context, AppSettings(darkMode = currentDarkMode, themeColorIndex = currentThemeColorIndex))
            }
        }
    }

    SettingsPageScaffold {
        ThemeSettingsCard(
            currentDarkMode = currentDarkMode,
            onDarkModeChange = { currentDarkMode = it; save(); onDarkModeChanged(it) },
            currentThemeColorIndex = currentThemeColorIndex,
            onThemeColorIndexChange = { currentThemeColorIndex = it; save(); onThemeChanged(it) }
        )
    }
}

@Composable
fun BackupSettingsPage() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showBackupConfirmDialog by remember { mutableStateOf(false) }
    var backupConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showWebDavConfigDialog by remember { mutableStateOf(false) }
    var webdavUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        webdavUrl = BackupManager.loadWebDavConfig(context).url
    }

    if (statusMessage != null) {
        StatusDialog(message = statusMessage!!, onDismiss = { statusMessage = null })
    }

    SettingsPageScaffold {
        BackupSettingsCard(
            context = context,
            isLoading = isLoading,
            onShowBackupConfirmDialogChange = { show, action ->
                showBackupConfirmDialog = show
                backupConfirmAction = action
            },
            onShowWebDavConfigDialogChange = { showWebDavConfigDialog = it },
            webdavUrl = webdavUrl,
            onStatusMessageChange = { statusMessage = it }
        )
    }

    if (showBackupConfirmDialog) {
        BackupConfirmDialog(
            onDismiss = { showBackupConfirmDialog = false; backupConfirmAction = null },
            onConfirm = {
                showBackupConfirmDialog = false
                backupConfirmAction?.invoke()
                backupConfirmAction = null
            }
        )
    }

    if (showWebDavConfigDialog) {
        WebDavConfigDialog(
            initialUrl = webdavUrl,
            onDismiss = { showWebDavConfigDialog = false },
            onSave = { webdavUrl = it.url }
        )
    }
}
