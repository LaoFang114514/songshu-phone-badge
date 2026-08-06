@file:OptIn(UnstableApi::class, DelicateCoroutinesApi::class)

package com.laofang.songshushoupai.songshu.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiAutoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.effect.Crop
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Transformer
import androidx.core.net.toUri
import androidx.core.graphics.scale

class VideoCropActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        try {
            enableEdgeToEdge()
            val idx = intent.getIntExtra("index", -1)
            val uri = intent.getStringExtra("uri")
            setContent {
                SongshushoupaiAutoTheme { VideoCropScreen(editIndex = idx, videoUri = uri, onFinish = { finish() }) }
            }
        } catch (_: Throwable) { finish() }
    }
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun VideoCropScreen(editIndex: Int, videoUri: String?, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val dm = ctx.resources.displayMetrics
    val sw = dm.widthPixels.toFloat()
    val sh = dm.heightPixels.toFloat()

    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var vidW by remember { mutableIntStateOf(0) }
    var vidH by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var bw by remember { mutableFloatStateOf(0f) }
    var bh by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1.2f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var processing by remember { mutableStateOf(false) }
    var tempFilePath by remember { mutableStateOf("") }
    var videoCoverPath by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processVideo(uri, ctx) { bmp, w, h, path, cover ->
                frame = bmp; vidW = w; vidH = h
                tempFilePath = path; videoCoverPath = cover
                loading = false
            }
        } else loading = false
    }

    LaunchedEffect(Unit) {
        if (editIndex >= 0) {
            val list = ImageDataManager.getImageList(ctx)
            if (editIndex < list.size && list[editIndex].filePath.isNotEmpty()) {
                tempFilePath = list[editIndex].filePath
                videoCoverPath = list[editIndex].coverPath
                withContext(Dispatchers.IO) {
                    try {
                        val r = MediaMetadataRetriever()
                        r.setDataSource(list[editIndex].filePath)
                        val bmp = r.getFrameAtTime(0)
                        val vw = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        val vh = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                        vidW = if (rot == 90 || rot == 270) vh else vw
                        vidH = if (rot == 90 || rot == 270) vw else vh
                        r.release()
                        frame = bmp
                    } catch (_: Throwable) {}
                }
                loading = false
            }
        }
        if (frame == null && videoUri != null) {
            val uri = videoUri.toUri()
            processVideo(uri, ctx) { bmp, w, h, path, cover ->
                frame = bmp; vidW = w; vidH = h
                tempFilePath = path; videoCoverPath = cover
                loading = false
            }
        }
        if (frame == null && videoUri == null) { loading = false; picker.launch("video/*") }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            return@Box
        }
        if (frame == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                Text(stringResource(R.string.no_video_selected), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { picker.launch("video/*") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) {
                    Icon(painter = painterResource(android.R.drawable.ic_input_add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.select_video))
                }
            }
            return@Box
        }

        val bitmap = frame!!
        val fitScale = minOf(bw / bitmap.width.toFloat(), bh / bitmap.height.toFloat())

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (!processing) onFinish() }) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                    }
                    Text(stringResource(R.string.video_crop_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RectangleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                val screenAspect = sw / sh
                val density = dm.density
                val statusBarH = with(LocalDensity.current) { WindowInsets.statusBars.getTop(LocalDensity.current).toFloat() }
                val topBarPx = 56f * density + statusBarH
                val bottomBarPx = 56f * density + 48f * density
                val availH = (sh - topBarPx - bottomBarPx).coerceAtLeast(100f)
                val cropW = minOf(sw * 1f, availH * 1f * screenAspect)
                val cropH = cropW / screenAspect
                Box(modifier = Modifier.width((cropW / density).dp).height((cropH / density).dp).clip(RectangleShape).background(Color.Black)
                    .onSizeChanged { bw = it.width.toFloat(); bh = it.height.toFloat() }) {
                    Canvas(modifier = Modifier.fillMaxSize().clip(RectangleShape)) {
                        val ts = fitScale * scale; val cx = size.width / 2; val cy = size.height / 2
                        val dw = bitmap.width * ts; val dh = bitmap.height * ts
                        drawImage(bitmap.asImageBitmap(), dstOffset = IntOffset((cx - dw / 2 + ox).toInt(), (cy - dh / 2 + oy).toInt()), dstSize = IntSize(dw.toInt(), dh.toInt()), filterQuality = FilterQuality.Medium)
                    }
                    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); ox += pan.x; oy += pan.y }
                    })
                    Box(modifier = Modifier.align(Alignment.Center).fillMaxSize().border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(4.dp)))
                    Canvas(modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(3.dp)) {
                        val cl = 20.dp.toPx(); val w = 2.dp.toPx(); val c = Color.White.copy(alpha = 0.9f)
                        drawLine(c, Offset(0f, cl), Offset(0f, 0f), w); drawLine(c, Offset(0f, 0f), Offset(cl, 0f), w)
                        drawLine(c, Offset(size.width - cl, 0f), Offset(size.width, 0f), w); drawLine(c, Offset(size.width, 0f), Offset(size.width, cl), w)
                        drawLine(c, Offset(0f, size.height - cl), Offset(0f, size.height), w); drawLine(c, Offset(0f, size.height), Offset(cl, size.height), w)
                        drawLine(c, Offset(size.width - cl, size.height), Offset(size.width, size.height), w); drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - cl), w)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { if (editIndex >= 0) onFinish() else picker.launch("video/*") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = !processing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))) { Text(if (editIndex >= 0) stringResource(R.string.cancel) else stringResource(R.string.reselect), fontSize = 15.sp) }
                    Button(onClick = {
                        if (processing) return@Button
                        if (bw > 0 && bh > 0 && vidW > 0 && vidH > 0 && tempFilePath.isNotEmpty()) {
                            processing = true
                            val ts = fitScale * scale
                            // 裁切框(view)对应 bitmap 的归一化区域
                            val nL = (0.5f - (bw / 2f + ox) / (bitmap.width * ts)).coerceIn(0f, 1f)
                            val nT = (0.5f - (bh / 2f + oy) / (bitmap.height * ts)).coerceIn(0f, 1f)
                            val nR = (0.5f + (bw / 2f - ox) / (bitmap.width * ts)).coerceIn(0f, 1f)
                            val nB = (0.5f + (bh / 2f - oy) / (bitmap.height * ts)).coerceIn(0f, 1f)
                            val isFullFrame = nL < 0.001f && nT < 0.001f && nR > 0.999f && nB > 0.999f
                            if (editIndex >= 0) {
                                if (isFullFrame) {
                                    onFinish()
                                } else {
                                    cropVideoFile(tempFilePath, nL, nT, nR, nB, ctx) { resultPath ->
                                        if (resultPath.isNotEmpty()) {
                                            val newCover = generateVideoCover(resultPath, ctx)
                                            val old = ImageDataManager.getImageList(ctx)[editIndex]
                                            if (old.filePath.isNotEmpty() && old.filePath != resultPath) File(old.filePath).delete()
                                            ImageDataManager.replaceImage(ctx, editIndex, resultPath, newCover)
                                        }
                                        onFinish()
                                    }
                                }
                            } else {
                                if (isFullFrame) {
                                    ImageDataManager.addVideoToList(ctx, tempFilePath, videoCoverPath)
                                    onFinish()
                                } else {
                                    cropVideoFile(tempFilePath, nL, nT, nR, nB, ctx) { resultPath ->
                                        if (resultPath.isNotEmpty()) {
                                            val newCover = generateVideoCover(resultPath, ctx)
                                            ImageDataManager.addVideoToList(ctx, resultPath, newCover)
                                            if (videoCoverPath.isNotEmpty() && videoCoverPath != newCover) File(videoCoverPath).delete()
                                        }
                                        onFinish()
                                    }
                                }
                            }
                        } else onFinish()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = !processing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.confirm_crop), fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
        if (processing) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp)); Text(stringResource(R.string.processing), color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@kotlin.OptIn(DelicateCoroutinesApi::class)
@Suppress("DEPRECATION")
private fun processVideo(uri: Uri, ctx: Context, onResult: (Bitmap?, Int, Int, String, String) -> Unit) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val tmpFile = File(ctx.cacheDir, "tmp_vid_${System.currentTimeMillis()}.mp4")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            }
            val r = MediaMetadataRetriever()
            r.setDataSource(tmpFile.absolutePath)
            val bmp = r.getFrameAtTime(0)
            val vw = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val vh = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val realW = if (rot == 90 || rot == 270) vh else vw
            val realH = if (rot == 90 || rot == 270) vw else vh
            var coverPath = ""
            try {
                bmp?.let { frame ->
                    val covDir = File(ctx.filesDir, "covers").also { it.mkdirs() }
                    val cf = File(covDir, "cover_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(cf).use { out -> frame.compress(Bitmap.CompressFormat.JPEG, 80, out) }
                    coverPath = cf.absolutePath
                }
            } catch (_: Throwable) {}
            r.release()
            withContext(Dispatchers.Main) { onResult(bmp, realW, realH, tmpFile.absolutePath, coverPath) }
        } catch (_: Throwable) {
            withContext(Dispatchers.Main) { onResult(null, 0, 0, "", "") }
        }
    }
}

@Suppress("DEPRECATION")
private fun generateVideoCover(videoPath: String, ctx: Context): String {
    return try {
        val r = MediaMetadataRetriever()
        r.setDataSource(videoPath)
        val bmp = r.getFrameAtTime(0)
        r.release()
        if (bmp == null) return ""
        val scaled = try {
            val maxDim = 1920
            if (bmp.width > maxDim || bmp.height > maxDim) {
                val s = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height)
                bmp.scale(
                    (bmp.width * s).toInt().coerceAtLeast(1),
                    (bmp.height * s).toInt().coerceAtLeast(1)
                )
            } else bmp
        } catch (_: Throwable) { bmp }
        val covDir = File(ctx.filesDir, "covers").also { it.mkdirs() }
        val cf = File(covDir, "cover_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cf).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        cf.absolutePath
    } catch (_: Throwable) { "" }
}

@Suppress("DEPRECATION")
private fun cropVideoFile(
    inputPath: String, nL: Float, nT: Float, nR: Float, nB: Float,
    ctx: Context,
    onResult: (String) -> Unit
) {
    val cropW = nR - nL
    val cropH = nB - nT
    if (cropW < 0.01f || cropH < 0.01f) {
        onResult(inputPath)
        return
    }

    val mainHandler = Handler(Looper.getMainLooper())
    val outputPath = File(
        File(ctx.filesDir, "videos").also { it.mkdirs() },
        "vid_${System.currentTimeMillis()}.mp4"
    ).absolutePath

    val r = MediaMetadataRetriever()
    val (encW, encH, rot) = try {
        r.setDataSource(inputPath)
        val vw = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val vh = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val ro = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        Triple(vw, vh, ro)
    } catch (_: Exception) {
        Triple(0, 0, 0)
    } finally {
        try { r.release() } catch (_: Exception) {}
    }

    val frameW = if (rot == 90 || rot == 270) encH else encW
    val frameH = if (rot == 90 || rot == 270) encW else encH
    if (frameW <= 0 || frameH <= 0) {
        onResult("")
        return
    }

    var pixL = ((nL * frameW).toInt() / 2) * 2
    var pixT = ((nT * frameH).toInt() / 2) * 2
    var pixR = (((nR * frameW).toInt() + 1) / 2) * 2
    var pixB = (((nB * frameH).toInt() + 1) / 2) * 2
    pixL = pixL.coerceIn(0, frameW - 2)
    pixT = pixT.coerceIn(0, frameH - 2)
    pixR = pixR.coerceIn(pixL + 2, frameW)
    pixB = pixB.coerceIn(pixT + 2, frameH)
    if ((pixR - pixL) % 2 != 0) pixR -= 1
    if ((pixB - pixT) % 2 != 0) pixB -= 1
    pixR = pixR.coerceAtLeast(pixL + 2)
    pixB = pixB.coerceAtLeast(pixT + 2)
    val cropEffect = Crop(
        pixL * 2f / frameW - 1f,
        pixR * 2f / frameW - 1f,
        1f - pixB * 2f / frameH,
        1f - pixT * 2f / frameH
    )
    val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(inputPath))
        .setEffects(Effects(listOf(), listOf(cropEffect)))
        .build()

    val composition = if (Build.VERSION.SDK_INT >= 29) {
        Composition.Builder(EditedMediaItemSequence(editedItem))
            .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
            .build()
    } else {
        Composition.Builder(EditedMediaItemSequence(editedItem)).build()
    }

    val transformer = Transformer.Builder(ctx)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: androidx.media3.transformer.ExportResult) {
                if (inputPath != outputPath) File(inputPath).delete()
                mainHandler.post { onResult(outputPath) }
            }
            override fun onError(composition: Composition, exportResult: androidx.media3.transformer.ExportResult, e: ExportException) {
                mainHandler.post { onResult("") }
            }
        })
        .build()
    try {
        transformer.start(composition, outputPath)
    } catch (_: Exception) {
        onResult("")
    }
}

