// 松鼠兽牌 一款旧手机改兽牌的小工具
// Copyright (C) 2026 laofang
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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.media.MediaMetadataRetriever
import com.laofang.songshushoupai.songshu.start.StartActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeColorIndex by remember {
                mutableIntStateOf(SettingsManager.loadSettings(context).themeColorIndex)
            }
            var darkMode by remember {
                mutableIntStateOf(SettingsManager.loadSettings(context).darkMode)
            }

            val useDarkTheme = when (darkMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            SongshushoupaiTheme(
                darkTheme = useDarkTheme,
                themeColorIndex = themeColorIndex
            ) {
                SongshushoupaiApp(
                    onThemeChanged = { newIndex ->
                        themeColorIndex = newIndex
                        val settings = SettingsManager.loadSettings(context)
                        SettingsManager.saveSettings(context, settings.copy(
                            themeColorIndex = newIndex
                        ))
                    },
                    onDarkModeChanged = { newMode ->
                        darkMode = newMode
                        val settings = SettingsManager.loadSettings(context)
                        SettingsManager.saveSettings(context, settings.copy(
                            darkMode = newMode
                        ))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun SongshushoupaiApp(
    onThemeChanged: (Int) -> Unit = {},
    onDarkModeChanged: (Int) -> Unit = {}
) {

    var currentDestination by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val imageList = remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    var basicConfigExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var aboutExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var qrExpanded by remember { mutableStateOf(false) }

    val settingsScrollState = remember { ScrollState(0) }
    val homeListState = remember { LazyListState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun refreshData() {
        imageList.value = ImageDataManager.getImageList(context)
        selectedIndex = ImageDataManager.getSelectedIndex(context)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it) ?: return@withContext
                        val dir = File(context.filesDir, "videos").also { d -> d.mkdirs() }
                        val file = File(dir, "vid_${System.currentTimeMillis()}.mp4")
                        FileOutputStream(file).use { out -> inputStream.copyTo(out) }
                        inputStream.close()

                        var coverPath = ""
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val frame = retriever.getFrameAtTime(0)
                            retriever.release()
                            if (frame != null) {
                                val coverDir = File(context.filesDir, "covers").also { d -> d.mkdirs() }
                                val coverFile = File(coverDir, "cover_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(coverFile).use { out ->
                                    frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                                }
                                coverPath = coverFile.absolutePath
                                frame.recycle()
                            }
                        } catch (_: Throwable) {}

                        ImageDataManager.addVideoToList(context, file.absolutePath, coverPath)
                    } catch (_: Throwable) {}
                }
                refreshData()
            }
        }
    }

    val observer = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                imageList.value = ImageDataManager.getImageList(context)
                selectedIndex = ImageDataManager.getSelectedIndex(context)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("松鼠兽牌") })
        },
        bottomBar = {
            NavigationBar {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = destination.label,
                                modifier = if (destination == AppDestinations.FAVORITES) {
                                    Modifier.size(40.dp)
                                } else {
                                    Modifier.size(24.dp)
                                }
                            )
                        },
                        selected = currentDestination == destination.ordinal,
                        onClick = {
                            if (destination == AppDestinations.FAVORITES) {
                                try {
                                    val intent = Intent(context, StartActivity::class.java)
                                    val options = ActivityOptionsCompat.makeCustomAnimation(
                                        context,
                                        android.R.anim.fade_in,
                                        android.R.anim.fade_out
                                    )
                                    context.startActivity(intent, options.toBundle())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                currentDestination = destination.ordinal
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                0 -> HomePage(
                    imageList = imageList.value,
                    selectedIndex = selectedIndex,
                    listState = homeListState,
                    onAddClick = {
                        val intent = Intent(context, CropActivity::class.java)
                        intent.putExtra("index", -1)
                        context.startActivity(intent)
                    },
                    onAddVideoClick = {
                        videoPickerLauncher.launch("video/*")
                    },
                    onSelect = { index ->
                        selectedIndex = index
                        ImageDataManager.setSelectedIndex(context, index)
                    },
                    onDelete = { index ->
                        ImageDataManager.deleteImage(context, index)
                        imageList.value = ImageDataManager.getImageList(context)
                        selectedIndex = ImageDataManager.getSelectedIndex(context)
                    },
                    onMoveUp = { index ->
                        if (index > 0) {
                            ImageDataManager.moveItem(context, index, index - 1)
                            imageList.value = ImageDataManager.getImageList(context)
                            selectedIndex = ImageDataManager.getSelectedIndex(context)
                        }
                    },
                    onMoveDown = { index ->
                        if (index < imageList.value.size - 1) {
                            ImageDataManager.moveItem(context, index, index + 1)
                            imageList.value = ImageDataManager.getImageList(context)
                            selectedIndex = ImageDataManager.getSelectedIndex(context)
                        }
                    },
                    onRename = { index, newName ->
                        ImageDataManager.renameItem(context, index, newName)
                        imageList.value = ImageDataManager.getImageList(context)
                    }
                )

                1 -> {}
                2 -> SettingsPage(
                    scrollState = settingsScrollState,
                    onImportComplete = ::refreshData,
                    basicConfigExpanded = basicConfigExpanded,
                    onBasicConfigExpandedChange = { basicConfigExpanded = it },
                    backupExpanded = backupExpanded,
                    onBackupExpandedChange = { backupExpanded = it },
                    aboutExpanded = aboutExpanded,
                    onAboutExpandedChange = { aboutExpanded = it },
                    themeExpanded = themeExpanded,
                    onThemeExpandedChange = { themeExpanded = it },
                    qrExpanded = qrExpanded,
                    onQrExpandedChange = { qrExpanded = it },
                    onThemeChanged = onThemeChanged,
                    onDarkModeChanged = onDarkModeChanged
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    imageList: List<ImageItem>,
    selectedIndex: Int,
    listState: LazyListState,
    onAddClick: () -> Unit,
    onAddVideoClick: () -> Unit,
    onSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRename: (Int, String) -> Unit
) {
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var renameIndex by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = remember { PaddingValues(
                horizontal = 16.dp, vertical = 8.dp
            ) }
        ) {
            itemsIndexed(
                items = imageList,
                key = { index, _ -> index }
            ) { index, item ->
                ImageCard(
                    item = item,
                    isSelected = item.index == selectedIndex,
                    onSelect = { onSelect(item.index) },
                    onDelete = { deleteIndex = item.index },
                    onMoveUp = { onMoveUp(item.index) },
                    onMoveDown = { onMoveDown(item.index) },
                    onRenameClick = { renameIndex = item.index },
                    canMoveUp = index > 0,
                    canMoveDown = index < imageList.size - 1
                )
            }

            item {
                AddImageCard(
                    onImageClick = onAddClick,
                    onVideoClick = onAddVideoClick
                )
            }
        }
    }

    if (deleteIndex >= 0 && deleteIndex < imageList.size) {
        AlertDialog(
            onDismissRequest = { deleteIndex = -1 },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${imageList[deleteIndex].name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleteIndex)
                    deleteIndex = -1
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndex = -1 }) { Text("取消") }
            }
        )
    }

    if (renameIndex >= 0 && renameIndex < imageList.size) {
        var text by remember { mutableStateOf(imageList[renameIndex].name) }
        AlertDialog(
            onDismissRequest = { renameIndex = -1 },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        onRename(renameIndex, text.trim())
                    }
                    renameIndex = -1
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { renameIndex = -1 }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AddImageCard(
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_input_add),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "添加兽牌",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("请选择展示方式") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = {
                            showDialog = false
                            onImageClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_gallery),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "图片",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Card(
                        onClick = {
                            showDialog = false
                            onVideoClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_slideshow),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "视频",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ImageCard(
    item: ImageItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRenameClick: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var bitmap by remember(item.filePath) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    val animatedBorderWidth by animateFloatAsState(
        targetValue = if (isSelected) 2f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "borderWidth"
    )

    val borderModifier = Modifier.border(
        BorderStroke(animatedBorderWidth.dp, MaterialTheme.colorScheme.primary),
        RoundedCornerShape(16.dp)
    )

    LaunchedEffect(item.filePath) {
        bitmap = withContext(Dispatchers.IO) {
            val path = if (item.isVideo) item.coverPath else item.filePath
            if (path.isNotEmpty()) {
                try {
                    val opts = android.graphics.BitmapFactory.Options()
                    opts.inJustDecodeBounds = true
                    android.graphics.BitmapFactory.decodeFile(path, opts)
                    if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                        null
                    } else {
                        var sampleSize = 1
                        val maxDim = 256
                        while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                            sampleSize *= 2
                        }
                        opts.inSampleSize = sampleSize
                        opts.inJustDecodeBounds = false
                        opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        android.graphics.BitmapFactory.decodeFile(path, opts)
                    }
                } catch (_: Throwable) {
                    null
                }
            } else null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(borderModifier)
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap!!.asImageBitmap()),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.shili),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (item.isVideo) {
                Text(
                    text = if (isSelected) "当前使用中" else "视频",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (isSelected) "当前使用中" else "图片",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "更多选项"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = { onRenameClick(); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("上移") },
                    onClick = { onMoveUp(); expanded = false },
                    enabled = canMoveUp
                )
                DropdownMenuItem(
                    text = { Text("下移") },
                    onClick = { onMoveDown(); expanded = false },
                    enabled = canMoveDown
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { onDelete(); expanded = false }
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("主页", R.drawable.ic_home),
    FAVORITES("启动", R.drawable.ic_favorite),
    PROFILE("设置", R.drawable.ic_account_box),

}

