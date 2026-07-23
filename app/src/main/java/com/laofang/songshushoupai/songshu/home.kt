package com.laofang.songshushoupai.songshu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.laofang.songshushoupai.songshu.start.StartActivity
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.core.view.WindowCompat


private val DlgShape = RoundedCornerShape(12.dp)

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val initSettings = remember { SettingsManager.loadSettings(ctx) }
            var themeIdx by remember { mutableIntStateOf(initSettings.themeColorIndex) }
            var darkMode by remember { mutableIntStateOf(initSettings.darkMode) }

            val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(owner) {
                val obs = LifecycleEventObserver { _, e ->
                    if (e == Lifecycle.Event.ON_RESUME) {
                        val s = SettingsManager.loadSettings(ctx)
                        themeIdx = s.themeColorIndex; darkMode = s.darkMode
                    }
                }
                owner.lifecycle.addObserver(obs)
                onDispose { owner.lifecycle.removeObserver(obs) }
            }

            val dark = when (darkMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            SongshushoupaiTheme(darkTheme = dark, themeColorIndex = themeIdx) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    @Suppress("DEPRECATION")
                    SideEffect {
                        val w = (view.context as? android.app.Activity)?.window ?: return@SideEffect
                        WindowCompat.getInsetsController(w, view).run {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                }
                SongshushoupaiApp(
                    onThemeChanged = {
                        themeIdx = it
                        val s = SettingsManager.loadSettings(ctx)
                        SettingsManager.saveSettings(ctx, s.copy(themeColorIndex = it))
                    },
                    onDarkModeChanged = {
                        darkMode = it
                        val s = SettingsManager.loadSettings(ctx)
                        SettingsManager.saveSettings(ctx, s.copy(darkMode = it))
                    }
                )
            }
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun SongshushoupaiApp(
    onThemeChanged: (Int) -> Unit = {},
    onDarkModeChanged: (Int) -> Unit = {}
) {
    var dest by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val imageList = remember { mutableStateOf(ImageDataManager.getImageList(ctx)) }
    var selIdx by remember { mutableIntStateOf(ImageDataManager.getSelectedIndex(ctx)) }
    var settingsSub by remember { mutableStateOf<String?>(null) }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    var lastBack by remember { mutableLongStateOf(0L) }

    BackHandler(dest == 2 && settingsSub != null) { settingsSub = null }
    BackHandler(dest != 2 || settingsSub == null) {
        val now = System.currentTimeMillis()
        if (now - lastBack < 2000) (ctx as? android.app.Activity)?.finish()
        else { lastBack = now; android.widget.Toast.makeText(ctx, "再按一次退出应用", android.widget.Toast.LENGTH_SHORT).show() }
    }

    fun refresh() {
        scope.launch {
            val (list, idx) = withContext(Dispatchers.IO) {
                ImageDataManager.getImageList(ctx) to ImageDataManager.getSelectedIndex(ctx)
            }
            imageList.value = list
            selIdx = idx
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val input = ctx.contentResolver.openInputStream(uri) ?: return@withContext
                    val file = File(File(ctx.filesDir, "videos").also { it.mkdirs() }, "vid_${System.currentTimeMillis()}.mp4")
                    FileOutputStream(file).use { out -> input.copyTo(out) }
                    input.close()
                    try {
                        val r = MediaMetadataRetriever()
                        r.setDataSource(file.absolutePath)
                        r.getFrameAtTime(0)?.let { frame ->
                            val cf = File(File(ctx.filesDir, "covers").also { it.mkdirs() }, "cover_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(cf).use { out -> frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out) }
                            ImageDataManager.addVideoToList(ctx, file.absolutePath, cf.absolutePath)
                            frame.recycle()
                        } ?: ImageDataManager.addVideoToList(ctx, file.absolutePath, "")
                        r.release()
                    } catch (_: Throwable) { ImageDataManager.addVideoToList(ctx, file.absolutePath, "") }
                } catch (_: Throwable) {}
            }
            refresh()
        }
    }

    val obs = remember { LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() } }
    DisposableEffect(owner) { owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            val title = if (dest == 2 && settingsSub != null) when (settingsSub) {
                "basic" -> "基本设置"; "qrcode" -> "二维码设置"; "theme" -> "主题设置"
                "backup" -> "数据备份"; "about" -> "关于软件"; else -> "松鼠兽牌"
            } else "松鼠兽牌"
            TopAppBar(title = { Text(title) })
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .background(colorScheme.surface)
            ) {
                HorizontalDivider(color = colorScheme.primary.copy(alpha = 0.3f))
                NavigationBar(containerColor = colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.height(70.dp)) {
                    AppDestinations.entries.forEach { d ->
                        NavigationBarItem(
                            icon = { Icon(painterResource(d.icon), d.label, modifier = if (d == AppDestinations.FAVORITES) Modifier.size(40.dp) else Modifier.size(24.dp)) },
                            selected = dest == d.ordinal,
                            onClick = {
                                if (d == AppDestinations.FAVORITES) {
                                    try {
                                        ctx.startActivity(Intent(ctx, StartActivity::class.java),
                                            ActivityOptionsCompat.makeCustomAnimation(ctx, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
                                    } catch (_: Exception) {}
                                } else dest = d.ordinal
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primary.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }
    ) { pad ->
        val homeAlpha by animateFloatAsState(
            targetValue = if (dest == 0) 1f else 0f,
            animationSpec = tween(200),
            label = "homeAlpha"
        )
        val settingsAlpha by animateFloatAsState(
            targetValue = if (dest == 2) 1f else 0f,
            animationSpec = tween(200),
            label = "settingsAlpha"
        )

        Box(Modifier.fillMaxSize().padding(pad)) {
            HomePage(imageList.value, selIdx, listState,
                onAddClick = { ctx.startActivity(Intent(ctx, CropActivity::class.java).putExtra("index", -1)) },
                onAddVideoClick = { videoPicker.launch("video/*") },
                onSelect = { selIdx = it; ImageDataManager.setSelectedIndex(ctx, it) },
                onDelete = { i -> ImageDataManager.deleteImage(ctx, i); refresh() },
                onMoveUp = { i -> if (i > 0) { ImageDataManager.moveItem(ctx, i, i - 1); refresh() } },
                onMoveDown = { i -> if (i < imageList.value.size - 1) { ImageDataManager.moveItem(ctx, i, i + 1); refresh() } },
                onRename = { i, n -> ImageDataManager.renameItem(ctx, i, n); imageList.value = ImageDataManager.getImageList(ctx) },
                modifier = Modifier.fillMaxSize().alpha(homeAlpha).zIndex(if (dest == 0) 1f else 0f)
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .alpha(settingsAlpha)
                    .zIndex(if (dest == 2) 1f else 0f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(settingsSub,
                    transitionSpec = {
                        val goingDeeper = initialState == null && targetState != null
                        val goingBack = initialState != null && targetState == null
                        if (goingDeeper) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else if (goingBack) {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        } else {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                        }
                    }, label = "settingsNav"
                ) { sub ->
                    when (sub) {
                        null -> SettingsPage(
                            onNavigateToBasicSettings = { settingsSub = "basic" },
                            onNavigateToQrCodeSettings = { settingsSub = "qrcode" },
                            onNavigateToThemeSettings = { settingsSub = "theme" },
                            onNavigateToBackupSettings = { settingsSub = "backup" },
                            onNavigateToAboutSettings = { settingsSub = "about" })
                        "basic" -> BasicSettingsPage()
                        "qrcode" -> QrCodeSettingsPage()
                        "theme" -> ThemeSettingsPage(onThemeChanged, onDarkModeChanged)
                        "backup" -> BackupSettingsPage(onDataChanged = { refresh() })
                        "about" -> AboutSettingsPage()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    imageList: List<ImageItem>, selectedIndex: Int, listState: LazyListState,
    onAddClick: () -> Unit, onAddVideoClick: () -> Unit,
    onSelect: (Int) -> Unit, onDelete: (Int) -> Unit,
    onMoveUp: (Int) -> Unit, onMoveDown: (Int) -> Unit, onRename: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var delIdx by remember { mutableIntStateOf(-1) }
    var renIdx by remember { mutableIntStateOf(-1) }

    Column(modifier) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = remember { PaddingValues(horizontal = 16.dp, vertical = 8.dp) }
        ) {
            itemsIndexed(imageList, key = { _, item -> item.index }) { i, item ->
                ImageCard(item, item.index == selectedIndex, { onSelect(item.index) },
                    { delIdx = item.index }, { onMoveUp(item.index) }, { onMoveDown(item.index) },
                    { renIdx = item.index }, i > 0, i < imageList.size - 1)
            }
            item { AddImageCard(onAddClick, onAddVideoClick) }
        }
    }

    if (delIdx in imageList.indices) {
        AlertDialog(onDismissRequest = { delIdx = -1 }, title = { Text("确认删除") },
            text = { Text("确定要删除「${imageList[delIdx].name}」吗？") },
            confirmButton = { Button(onClick = { onDelete(delIdx); delIdx = -1 }, shape = DlgShape) { Text("删除") } },
            dismissButton = { OutlinedButton(onClick = { delIdx = -1 }, shape = DlgShape) { Text("取消") } })
    }
    if (renIdx in imageList.indices) {
        var txt by remember { mutableStateOf(imageList[renIdx].name) }
        AlertDialog(onDismissRequest = { renIdx = -1 }, title = { Text("重命名") },
            text = { OutlinedTextField(txt, { txt = it }, singleLine = true, label = { Text("名称") }) },
            confirmButton = { Button(onClick = { if (txt.isNotBlank()) onRename(renIdx, txt.trim()); renIdx = -1 }, shape = DlgShape) { Text("确认") } },
            dismissButton = { OutlinedButton(onClick = { renIdx = -1 }, shape = DlgShape) { Text("取消") } })
    }
}

@Composable
private fun AddImageCard(onImageClick: () -> Unit, onVideoClick: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) colorScheme.surface else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            .clickable { show = true },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Add, null, Modifier.size(40.dp), colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("添加兽牌", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
    }

    if (show) {
        AlertDialog(onDismissRequest = { show = false }, title = { Text("请选择展示方式") },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        PickCard(onClick = { show = false; onImageClick() }, icon = Icons.Filled.Image,
                            label = "图片", tint = colorScheme.primary, modifier = Modifier.weight(1f).height(120.dp))
                        PickCard(onClick = { show = false; onVideoClick() }, icon = Icons.Filled.Videocam,
                            label = "视频", tint = colorScheme.tertiary, modifier = Modifier.weight(1f).height(120.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { Button(onClick = { show = false }, modifier = Modifier.fillMaxWidth(), shape = DlgShape) { Text("取消") } })
    }
}

@Composable
private fun PickCard(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier = Modifier) {
    Card(onClick, modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, colorScheme.outlineVariant)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, Modifier.size(40.dp), tint)
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ImageCard(
    item: ImageItem, isSelected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRenameClick: () -> Unit,
    canMoveUp: Boolean, canMoveDown: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var bitmap by remember(item.filePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val borderWidth by animateFloatAsState(if (isSelected) 2f else 1f, tween(250), label = "borderWidth")

    Row(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp))
        .background(colorScheme.surface)
        .border(BorderStroke(borderWidth.dp, if (isSelected) colorScheme.primary else colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
        .clickable { onSelect() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {

        Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant)) {
            val bmp = bitmap
            if (bmp != null) Image(BitmapPainter(bmp.asImageBitmap()), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.shili), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isSelected) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(colorScheme.primary))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(if (isSelected) "当前使用中" else if (item.isVideo) "视频" else "图片",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant)
        }

        Box {
            IconButton({ expanded = true }) { Icon(Icons.Filled.MoreVert, "更多选项") }
            DropdownMenu(expanded, { expanded = false }, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(BorderStroke(1.dp, colorScheme.outlineVariant), RoundedCornerShape(12.dp))) {
                DropdownMenuItem({ Text("重命名") }, onClick = { onRenameClick(); expanded = false })
                DropdownMenuItem({ Text("上移") }, onClick = { onMoveUp(); expanded = false }, enabled = canMoveUp)
                DropdownMenuItem({ Text("下移") }, onClick = { onMoveDown(); expanded = false }, enabled = canMoveDown)
                DropdownMenuItem({ Text("删除") }, onClick = { onDelete(); expanded = false })
            }
        }
    }

    LaunchedEffect(item.filePath) {
        bitmap = withContext(Dispatchers.IO) {
            val path = if (item.isVideo) item.coverPath else item.filePath
            if (path.isEmpty()) return@withContext null
            decodeBitmapSampled(path, 256, android.graphics.Bitmap.Config.RGB_565)
        }
    }
}

enum class AppDestinations(val label: String, val icon: Int) {
    HOME("主页", R.drawable.ic_home),
    FAVORITES("启动", R.drawable.ic_favorite),
    PROFILE("设置", R.drawable.ic_account_box),
}

