// 松鼠兽牌 一款旧手机变兽牌的软件
// Copyright (C) 2026  laofang
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.laofang.songshushoupai.songshu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.stringResource
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
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.laofang.songshushoupai.songshu.start.StartActivity
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.compose.ui.text.buildAnnotatedString
import com.laofang.songshushoupai.songshu.core.CropActivity
import com.laofang.songshushoupai.songshu.core.ImageDataManager
import com.laofang.songshushoupai.songshu.core.ImageItem
import com.laofang.songshushoupai.songshu.core.LocaleHelper
import com.laofang.songshushoupai.songshu.core.SettingsManager
import com.laofang.songshushoupai.songshu.core.UpdateChecker
import com.laofang.songshushoupai.songshu.core.UpdateInfo
import com.laofang.songshushoupai.songshu.core.decodeBitmapSampled

private val DlgShape = RoundedCornerShape(12.dp)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val settings = remember { SettingsManager.loadSettings(ctx) }
            var themeIdx by remember { mutableIntStateOf(settings.themeColorIndex) }
            var darkMode by remember { mutableIntStateOf(settings.darkMode) }
            var prevLangIdx by remember { mutableIntStateOf(settings.languageIndex) }

            val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(owner) {
                val obs = LifecycleEventObserver { _, e ->
                    if (e == Lifecycle.Event.ON_RESUME) {
                        val s = SettingsManager.loadSettings(ctx)
                        themeIdx = s.themeColorIndex; darkMode = s.darkMode
                        if (s.languageIndex != prevLangIdx) {
                            prevLangIdx = s.languageIndex
                            (ctx as? android.app.Activity)?.recreate()
                        }
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
    var dest by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val imageList = remember { mutableStateOf(ImageDataManager.getImageList(ctx)) }
    var selIdx by remember { mutableIntStateOf(ImageDataManager.getSelectedIndex(ctx)) }
    var settingsSub by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsSubPrevious by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    var lastBack by remember { mutableLongStateOf(0L) }
    var updatePopupInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updatePopupShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UpdateChecker.clearCacheIfVersionChanged(ctx)
        val result = UpdateChecker.checkForUpdate(ctx, BuildConfig.VERSION_NAME)
        val info = result.getOrNull()
        if (info != null && !updatePopupShown && UpdateChecker.shouldAutoShowPopup(ctx)) {
            updatePopupInfo = info
            updatePopupShown = true
        }
    }

    fun dismissUpdatePopup() {
        UpdateChecker.dismissPopup(ctx)
        updatePopupInfo = null
    }

    BackHandler(dest == 2 && settingsSub != null) {
        settingsSub = settingsSubPrevious
        settingsSubPrevious = null
    }
    BackHandler(dest != 2 || settingsSub == null) {
        val now = System.currentTimeMillis()
        if (now - lastBack < 2000) (ctx as? android.app.Activity)?.finish()
        else { lastBack = now; android.widget.Toast.makeText(ctx, ctx.getString(R.string.press_again_exit), android.widget.Toast.LENGTH_SHORT).show() }
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

    val obs = remember { LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() } }
    DisposableEffect(owner) { owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            val title = if (dest == 2 && settingsSub != null) when (settingsSub) {
                "basic" -> stringResource(R.string.basic_settings); "qrcode" -> stringResource(R.string.qrcode_settings); "theme" -> stringResource(R.string.theme_settings)
                "backup" -> stringResource(R.string.backup_settings); "about" -> stringResource(R.string.about_settings); "tutorial" -> stringResource(R.string.tutorial_settings)
                "license" -> stringResource(R.string.open_source_license); else -> stringResource(R.string.app_name)
            } else stringResource(R.string.app_name)
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
                            icon = { Icon(painterResource(d.icon), stringResource(d.labelRes), modifier = if (d == AppDestinations.FAVORITES) Modifier.size(40.dp) else Modifier.size(24.dp)) },
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
                onAddVideoClick = { ctx.startActivity(Intent(ctx, com.laofang.songshushoupai.songshu.core.VideoCropActivity::class.java).putExtra("index", -1)) },
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
                        val goingDeeper = (initialState == null && targetState != null) ||
                                (initialState == "about" && targetState == "license")
                        val goingBack = (initialState != null && targetState == null) ||
                                (initialState == "license" && targetState == "about")
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
                            onNavigateToAboutSettings = { settingsSub = "about" },
                            onNavigateToTutorial = { settingsSub = "tutorial" },
                            onUpdateClick = { updatePopupInfo = it })
                        "basic" -> BasicSettingsPage()
                        "qrcode" -> QrCodeSettingsPage()
                        "theme" -> ThemeSettingsPage(onThemeChanged, onDarkModeChanged)
                        "backup" -> BackupSettingsPage(onDataChanged = { refresh() })
                        "about" -> AboutSettingsPage(onOpenLicense = {
                            settingsSubPrevious = "about"
                            settingsSub = "license"
                        })
                        "tutorial" -> TutorialSettingsPage()
                        "license" -> LicenseSettingsPage()
                    }
                }
            }

            if (updatePopupInfo != null) {
                val info = updatePopupInfo!!
                AlertDialog(
                    onDismissRequest = { dismissUpdatePopup() },
                    title = { Text(stringResource(R.string.new_version_found, info.version)) },
                    text = {
                        if (info.description.isNotBlank()) {
                            HtmlDescriptionText(html = info.description)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                dismissUpdatePopup()
                                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, info.link.toUri())) } catch (_: Exception) {}
                            },
                            shape = DlgShape
                        ) { Text(stringResource(R.string.check_update)) }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { dismissUpdatePopup() }, shape = DlgShape) { Text(stringResource(R.string.maybe_later)) }
                    }
                )
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
        AlertDialog(onDismissRequest = { delIdx = -1 }, title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_msg, imageList[delIdx].name)) },
            confirmButton = { Button(onClick = { onDelete(delIdx); delIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.delete)) } },
            dismissButton = { OutlinedButton(onClick = { delIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.cancel)) } })
    }
    if (renIdx in imageList.indices) {
        var txt by remember { mutableStateOf(imageList[renIdx].name) }
        AlertDialog(onDismissRequest = { renIdx = -1 }, title = { Text(stringResource(R.string.rename)) },
            text = { OutlinedTextField(txt, { txt = it }, singleLine = true, label = { Text(stringResource(R.string.name_label)) }) },
            confirmButton = { Button(onClick = { if (txt.isNotBlank()) onRename(renIdx, txt.trim()); renIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { OutlinedButton(onClick = { renIdx = -1 }, shape = DlgShape) { Text(stringResource(R.string.cancel)) } })
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
            Text(stringResource(R.string.add_badge), style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
    }

    if (show) {
        AlertDialog(onDismissRequest = { show = false }, title = { Text(stringResource(R.string.pick_display_mode)) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        PickCard(onClick = { show = false; onImageClick() }, icon = Icons.Filled.Image,
                            label = stringResource(R.string.image), tint = colorScheme.primary, modifier = Modifier.weight(1f).height(120.dp))
                        PickCard(onClick = { show = false; onVideoClick() }, icon = Icons.Filled.Videocam,
                            label = stringResource(R.string.video), tint = colorScheme.tertiary, modifier = Modifier.weight(1f).height(120.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { Button(onClick = { show = false }, modifier = Modifier.fillMaxWidth(), shape = DlgShape) { Text(stringResource(R.string.cancel)) } })
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
    var bitmap by remember(item.filePath) { mutableStateOf<Bitmap?>(null) }
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
            Text(if (isSelected) stringResource(R.string.currently_in_use) else if (item.isVideo) stringResource(R.string.video) else stringResource(R.string.image),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant)
        }

        Box {
            IconButton({ expanded = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
            DropdownMenu(expanded, { expanded = false }, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(BorderStroke(1.dp, colorScheme.outlineVariant), RoundedCornerShape(12.dp))) {
                DropdownMenuItem({ Text(stringResource(R.string.rename)) }, onClick = { onRenameClick(); expanded = false })
                DropdownMenuItem({ Text(stringResource(R.string.move_up)) }, onClick = { onMoveUp(); expanded = false }, enabled = canMoveUp)
                DropdownMenuItem({ Text(stringResource(R.string.move_down)) }, onClick = { onMoveDown(); expanded = false }, enabled = canMoveDown)
                DropdownMenuItem({ Text(stringResource(R.string.delete)) }, onClick = { onDelete(); expanded = false })
            }
        }
    }

    LaunchedEffect(item.filePath) {
        bitmap = withContext(Dispatchers.IO) {
            val path = if (item.isVideo) item.coverPath else item.filePath
            if (path.isEmpty()) return@withContext null
            decodeBitmapSampled(path, 256, Bitmap.Config.RGB_565)
        }
    }
}

enum class AppDestinations(val labelRes: Int, val icon: Int) {
    HOME(R.string.nav_home, R.drawable.ic_home),
    FAVORITES(R.string.nav_start, R.drawable.ic_favorite),
    PROFILE(R.string.nav_settings, R.drawable.ic_account_box),
}

@Composable
private fun HtmlDescriptionText(html: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val annotated = remember(html) {
        buildAnnotatedString {
            @Suppress("DEPRECATION")
            val spanned = if (android.os.Build.VERSION.SDK_INT >= 24)
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
            else
                android.text.Html.fromHtml(html)
            append(spanned.toString())
            val urlSpans = spanned.getSpans(0, spanned.length, android.text.style.URLSpan::class.java)
            for (span in urlSpans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                addLink(
                    androidx.compose.ui.text.LinkAnnotation.Url(span.url) {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, span.url.toUri())) } catch (_: Exception) {}
                    },
                    start, end
                )
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
        maxLines = 8,
        modifier = modifier
    )
}
// QWQ