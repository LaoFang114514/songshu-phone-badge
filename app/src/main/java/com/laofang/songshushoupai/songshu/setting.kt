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
import androidx.compose.ui.res.stringResource
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
import com.laofang.songshushoupai.songshu.settings.TutorialSettingsCard
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import java.util.Calendar
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.laofang.songshushoupai.songshu.core.BackupManager
import com.laofang.songshushoupai.songshu.core.QrCodeDataManager
import com.laofang.songshushoupai.songshu.core.SettingsManager
import com.laofang.songshushoupai.songshu.core.UpdateChecker
import com.laofang.songshushoupai.songshu.core.UpdateInfo
import com.laofang.songshushoupai.songshu.core.WebDavConfig

private val CardShape = RoundedCornerShape(16.dp)
private val BtnShape = RoundedCornerShape(12.dp)

private fun isAprilFools(): Boolean {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.MONTH) == Calendar.APRIL && cal.get(Calendar.DAY_OF_MONTH) == 1
}

@Composable
private fun cardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingsPageScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
        title = { Text(stringResource(R.string.tip)) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onDismiss, shape = BtnShape) { Text(stringResource(R.string.ok)) } },
        shape = BtnShape
    )
}

@Composable
private fun BackupConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_action)) },
        text = { Text(stringResource(R.string.confirm_override_data)) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = BtnShape) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onConfirm, shape = BtnShape) { Text(stringResource(R.string.ok)) }
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
    onNavigateToAboutSettings: () -> Unit = {},
    onNavigateToTutorial: () -> Unit = {},
    onUpdateClick: (UpdateInfo) -> Unit = {}
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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300.milliseconds)
        doCheckUpdate(false)
    }

    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val prefs = ctx.getSharedPreferences("update_state", Context.MODE_PRIVATE)
                val lastVer = prefs.getString("last_version", "")
                val curVer = BuildConfig.VERSION_NAME
                if (lastVer != "" && lastVer != curVer) {
                    updateInfo = null
                }
                prefs.edit { putString("last_version", curVer) }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

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
                val aprilFools = isAprilFools()
                // 付费纯愚人节玩笑，请勿当真
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (aprilFools) stringResource(R.string.trial_expiring) else stringResource(R.string.support_developer), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { doCheckUpdate(true) }
                    ) {
                        Text(
                            if (checkingUpdate) stringResource(R.string.checking_update) else "V${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(if (aprilFools) stringResource(R.string.please_purchase) else stringResource(R.string.sponsor_via), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { openUrl(ctx, "https://www.ifdian.net/a/laofang") }, Modifier.weight(1f), shape = BtnShape) { Text(if (aprilFools) stringResource(R.string.purchase_channel_1) else stringResource(R.string.ifdian)) }
                    OutlinedButton(onClick = { openUrl(ctx, "https://ko-fi.com/laofang") }, Modifier.weight(1f), shape = BtnShape) { Text(if (aprilFools) stringResource(R.string.purchase_channel_2) else "Ko-fi") }

                }
            }
        }

        updateInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape)
                    .clickable { onUpdateClick(info) },
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.new_version_title, info.version), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(" ▶", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        var emergencyExpanded by remember {
            mutableStateOf(ctx.getSharedPreferences("ui_state", Context.MODE_PRIVATE).getBoolean("emergency_expanded", true))
        }
        Card(
            modifier = Modifier.fillMaxWidth().border(cardBorder(), CardShape).clip(CardShape),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        emergencyExpanded = !emergencyExpanded
                        ctx.getSharedPreferences("ui_state", Context.MODE_PRIVATE).edit { putBoolean("emergency_expanded", emergencyExpanded) }
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.temp_notice), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                            stringResource(R.string.notice_content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { openUrl(ctx, "https://h5.qzone.qq.com/ugc/share/?sharetag=9653E56224DEB4DF2573CB788FF50CB2&subtype=&ciphertext=&sid=&blog_photo=&g=84&res_uin=2908807760&cellid=50e260ad98d85c6ac7a40a00&subid=&bp1=&bp2=&bp7=&appid=311#wechat_qqauth&wechat_redirect") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = BtnShape
                        ) { Text(stringResource(R.string.learn_more)) }

                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        NavRow(stringResource(R.string.basic_settings)) { onNavigateToBasicSettings() }
        NavRow(stringResource(R.string.qrcode_settings)) { onNavigateToQrCodeSettings() }
        NavRow(stringResource(R.string.theme_settings)) { onNavigateToThemeSettings() }
        NavRow(stringResource(R.string.backup_settings)) { onNavigateToBackupSettings() }
        NavRow(stringResource(R.string.tutorial_settings)) { onNavigateToTutorial() }
        NavRow(stringResource(R.string.about_settings)) { onNavigateToAboutSettings() }

    }
}

@Composable
fun AboutSettingsPage(onOpenLicense: () -> Unit = {}) {
    SettingsPageScaffold { AboutSettingsCard(onOpenLicense = onOpenLicense) }
}

@Composable
fun LicenseSettingsPage() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val libs = listOf(
        Triple(R.string.lib_compose, R.string.lib_compose_desc, "https://github.com/JetBrains/compose-multiplatform"),
        Triple(R.string.lib_media3, R.string.lib_media3_desc, "https://github.com/androidx/media3"),
        Triple(R.string.lib_material, R.string.lib_material_desc, "https://github.com/material-components/material-components-android"),
        Triple(R.string.lib_kotlin, R.string.lib_kotlin_desc, "https://github.com/JetBrains/kotlin"),
        Triple(R.string.lib_zxing, R.string.lib_zxing_desc, "https://github.com/zxing/zxing")
    )
    SettingsPageScaffold {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            libs.forEach { (nameRes, descRes, url) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                        catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(nameRes),
                                style = MaterialTheme.typography.bodyLarge, color = cs.primary)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Text("›", style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TutorialSettingsPage() {
    SettingsPageScaffold { TutorialSettingsCard() }
}

@Composable
private fun <T> SettingsPageHost(
    load: (Context) -> T,
    save: (Context, T) -> Unit,
    content: @Composable (state: T, onChange: (T.() -> T) -> Unit) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(load(ctx)) }
    fun update(transform: T.() -> T) {
        state = state.transform()
        scope.launch { withContext(Dispatchers.IO) { save(ctx, state) } }
    }
    SettingsPageScaffold { content(state, ::update) }
}

@Composable
fun BasicSettingsPage() = SettingsPageHost(
    load = { SettingsManager.loadSettings(it) },
    save = { ctx, s -> SettingsManager.saveSettings(ctx, s) }
) { s, update ->
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    BasicSettingsCard(
        defaultOrientation = s.defaultOrientation, onDefaultOrientationChange = { update { copy(defaultOrientation = it) } },
        keepScreenOn = s.keepScreenOn, onKeepScreenOnChange = { update { copy(keepScreenOn = it) } },
        showBattery = s.showBattery, onShowBatteryChange = { update { copy(showBattery = it) } },
        lockOrientation = s.lockOrientation, onLockOrientationChange = { update { copy(lockOrientation = it) } },
        antiBurnIn = s.antiBurnIn, onAntiBurnInChange = { update { copy(antiBurnIn = it) } },
        muteVideo = s.muteVideo, onMuteVideoChange = { update { copy(muteVideo = it) } },
        languageIndex = s.languageIndex, onLanguageChange = {
            update { copy(languageIndex = it) }
            scope.launch {
                kotlinx.coroutines.delay(500.milliseconds)
                (ctx as? android.app.Activity)?.recreate()
            }
        }
    )
}

@Composable
fun QrCodeSettingsPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQrCode by remember { mutableStateOf(SettingsManager.loadSettings(ctx).showQrCode) }
    val qrList = remember { mutableStateOf(QrCodeDataManager.getQrList(ctx)) }
    var selIdx by remember { mutableIntStateOf(QrCodeDataManager.getSelectedIndex(ctx)) }

    fun refresh() {
        scope.launch {
            withContext(Dispatchers.IO) {
                qrList.value = QrCodeDataManager.getQrList(ctx)
                selIdx = QrCodeDataManager.getSelectedIndex(ctx)
            }
        }
    }

    SettingsPageScaffold {
        QrCodeSettingsCard(
            showQrCode = showQrCode, onShowQrCodeChange = {
                showQrCode = it
                scope.launch { withContext(Dispatchers.IO) {
                    val s = SettingsManager.loadSettings(ctx)
                    SettingsManager.saveSettings(ctx, s.copy(showQrCode = it))
                }}
            },
            qrList = qrList.value, selectedIndex = selIdx,
            onSelect = { selIdx = it; QrCodeDataManager.setSelectedIndex(ctx, it) },
            onAdd = { item ->
                scope.launch(Dispatchers.IO) {
                    val currentList = QrCodeDataManager.getQrList(ctx)
                    val nextNum = currentList.size + 1
                    val namedItem = if (item.name.isEmpty()) item.copy(name = ctx.getString(R.string.qr_default_name, nextNum)) else item
                    QrCodeDataManager.addItem(ctx, namedItem)
                    val newIdx = QrCodeDataManager.getQrList(ctx).size - 1
                    QrCodeDataManager.setSelectedIndex(ctx, newIdx)
                    qrList.value = QrCodeDataManager.getQrList(ctx)
                    selIdx = QrCodeDataManager.getSelectedIndex(ctx)
                }
            },
            onDelete = { QrCodeDataManager.deleteItem(ctx, it); refresh() },
            onMoveUp = { if (it > 0) { QrCodeDataManager.moveItem(ctx, it, it - 1); refresh() } },
            onMoveDown = { if (it < qrList.value.size - 1) { QrCodeDataManager.moveItem(ctx, it, it + 1); refresh() } },
            onRename = { i, n -> QrCodeDataManager.renameItem(ctx, i, n); refresh() }
        )
    }
}

@Composable
fun ThemeSettingsPage(onThemeChanged: (Int) -> Unit, onDarkModeChanged: (Int) -> Unit) = SettingsPageHost(
    load = { SettingsManager.loadSettings(it) },
    save = { ctx, s -> SettingsManager.saveSettings(ctx, s) }
) { s, update ->
    ThemeSettingsCard(
        currentDarkMode = s.darkMode,
        onDarkModeChange = { update { copy(darkMode = it) }; onDarkModeChanged(it) },
        currentThemeColorIndex = s.themeColorIndex,
        onThemeColorIndexChange = { update { copy(themeColorIndex = it) }; onThemeChanged(it) }
    )
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
                try { BackupManager.exportToZip(appCtx, uri); ERR_EXPORT_SUCCESS } catch (e: Exception) { "$ERR_EXPORT_FAILED:${e.localizedMessage}" }
            }
            statusMsg = msg; isLoading = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isLoading = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { BackupManager.importFromZip(appCtx, uri) }
            statusMsg = if (ok) ERR_IMPORT_SUCCESS else ERR_IMPORT_FAILED; isLoading = false
            if (ok) onDataChanged()
        }
    }

    fun exec(op: BackupOperation) {
        when (op) {
            BackupOperation.EXPORT -> exportLauncher.launch("songshushoupai_backup.zip")
            BackupOperation.IMPORT -> importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            BackupOperation.WEBDAV_UPLOAD, BackupOperation.WEBDAV_DOWNLOAD -> {
                val cfg = BackupManager.loadWebDavConfig(appCtx)
                if (cfg.url.isBlank()) { statusMsg = ERR_CONFIG_SERVER_FIRST; return }
                isLoading = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        if (op == BackupOperation.WEBDAV_UPLOAD) BackupManager.webdavUpload(appCtx, cfg)
                        else BackupManager.webdavDownload(appCtx, cfg)
                    }
                    statusMsg = result ?: if (op == BackupOperation.WEBDAV_UPLOAD) ERR_BACKUP_SUCCESS else ERR_RESTORE_SUCCESS
                    isLoading = false
                    if (result == null && op == BackupOperation.WEBDAV_DOWNLOAD) onDataChanged()
                }
            }
        }
    }

    statusMsg?.let { StatusDialog(message = backupErrorMsg(it), onDismiss = { statusMsg = null }) }

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
                if (webdavUrl.isBlank()) { statusMsg = ERR_FILL_SERVER; return@BackupSettingsCard }
                isTesting = true
                scope.launch {
                    val err = BackupManager.webdavTestConnection(
                        WebDavConfig(
                            webdavUrl,
                            webdavUser,
                            webdavPass
                        )
                    )
                    statusMsg = err ?: ERR_CONNECTION_SUCCESS
                    isTesting = false
                }
            },
            onSaveWebDavConfig = {
                BackupManager.saveWebDavConfig(appCtx,
                    WebDavConfig(webdavUrl, webdavUser, webdavPass)
                )
                savedUrl = webdavUrl; savedUser = webdavUser; savedPass = webdavPass
                statusMsg = ERR_WEBDAV_CONFIG_SAVED
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

private const val ERR_EXPORT_SUCCESS = "_export_ok"
private const val ERR_EXPORT_FAILED = "_export_fail"
private const val ERR_IMPORT_SUCCESS = "_import_ok"
private const val ERR_IMPORT_FAILED = "_import_fail"
private const val ERR_CONFIG_SERVER_FIRST = "_config_server"
private const val ERR_BACKUP_SUCCESS = "_backup_ok"
private const val ERR_RESTORE_SUCCESS = "_restore_ok"
private const val ERR_FILL_SERVER = "_fill_server"
private const val ERR_CONNECTION_SUCCESS = "_conn_ok"
private const val ERR_WEBDAV_CONFIG_SAVED = "_webdav_saved"

@Composable
private fun backupErrorMsg(code: String?): String {
    if (code == null) return ""
    val ctx = LocalContext.current
    return when {
        code == BackupManager.ERR_AUTH_FAIL -> ctx.getString(R.string.err_auth_fail)
        code == BackupManager.ERR_NO_PERMISSION -> ctx.getString(R.string.err_no_permission)
        code.startsWith(BackupManager.ERR_CONNECTION_FAILED) -> {
            val parts = code.split(":", limit = 2)
            val detail = if (parts.size > 1) parts[1] else code
            ctx.getString(R.string.err_connection_failed, detail)
        }
        code == BackupManager.ERR_NETWORK_ERROR -> ctx.getString(R.string.err_network_error)
        code == BackupManager.ERR_UPLOAD_AUTH_FAIL -> ctx.getString(R.string.err_upload_auth_fail)
        code == BackupManager.ERR_UPLOAD_NO_PERMISSION -> ctx.getString(R.string.err_upload_no_permission)
        code.startsWith(BackupManager.ERR_UPLOAD_FAILED) -> {
            val parts = code.split(":", limit = 2)
            val detail = if (parts.size > 1) parts[1] else code
            ctx.getString(R.string.err_upload_failed, detail)
        }
        code == BackupManager.ERR_UPLOAD_FAILED_SIMPLE -> ctx.getString(R.string.err_upload_failed_simple)
        code == BackupManager.ERR_REDIRECT_NO_LOCATION -> ctx.getString(R.string.err_redirect_no_location)
        code == BackupManager.ERR_BACKUP_NOT_FOUND -> ctx.getString(R.string.err_backup_not_found)
        code == BackupManager.ERR_DOWNLOAD_NO_PERMISSION -> ctx.getString(R.string.err_download_no_permission)
        code.startsWith(BackupManager.ERR_SERVER_RESPONSE) -> {
            val parts = code.split(":", limit = 2)
            val detail = if (parts.size > 1) parts[1] else code
            ctx.getString(R.string.err_server_response, detail)
        }
        code == BackupManager.ERR_DOWNLOADED_EMPTY -> ctx.getString(R.string.err_downloaded_empty)
        code == BackupManager.ERR_NO_CONFIG_IN_ZIP -> ctx.getString(R.string.err_no_config_in_zip)
        code == BackupManager.ERR_UNKNOWN_ERROR -> ctx.getString(R.string.err_unknown_error)
        code == ERR_EXPORT_SUCCESS -> ctx.getString(R.string.export_success)
        code.startsWith(ERR_EXPORT_FAILED) -> {
            val detail = code.split(":", limit = 2).getOrElse(1) { "" }
            ctx.getString(R.string.export_failed, detail)
        }
        code == ERR_IMPORT_SUCCESS -> ctx.getString(R.string.import_success)
        code == ERR_IMPORT_FAILED -> ctx.getString(R.string.import_failed)
        code == ERR_CONFIG_SERVER_FIRST -> ctx.getString(R.string.config_server_first)
        code == ERR_BACKUP_SUCCESS -> ctx.getString(R.string.backup_success)
        code == ERR_RESTORE_SUCCESS -> ctx.getString(R.string.restore_success)
        code == ERR_FILL_SERVER -> ctx.getString(R.string.fill_server_address)
        code == ERR_CONNECTION_SUCCESS -> ctx.getString(R.string.connection_success)
        code == ERR_WEBDAV_CONFIG_SAVED -> ctx.getString(R.string.webdav_config_saved)
        else -> code
    }
}
