package com.laofang.songshushoupai.songshu

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiTheme

class CropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            val idx = intent.getIntExtra("index", -1)
            setContent {
                val ctx = LocalContext.current
                val s = remember { SettingsManager.loadSettings(ctx) }
                val dark = when (s.darkMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
                SongshushoupaiTheme(darkTheme = dark, themeColorIndex = s.themeColorIndex) {
                    CropScreen(editIndex = idx, onFinish = { finish() })
                }
            }
        } catch (e: Throwable) { android.util.Log.e("CropActivity", "onCreate error", e); finish() }
    }
}

private fun loadBitmap(uri: android.net.Uri, ctx: android.content.Context): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val dec = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        ctx.contentResolver.openInputStream(uri)?.use {
            try { BitmapFactory.decodeStream(it, null, dec) } catch (_: Throwable) {
                dec.inSampleSize *= 2
                ctx.contentResolver.openInputStream(uri)?.use { s -> try { BitmapFactory.decodeStream(s, null, dec) } catch (_: Throwable) { null } }
            }
        }
    } catch (_: Throwable) { null }
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun CropScreen(editIndex: Int, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val sw = ctx.resources.displayMetrics.widthPixels.toFloat()
    val sh = ctx.resources.displayMetrics.heightPixels.toFloat()

    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var bw by remember { mutableFloatStateOf(0f) }
    var bh by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { try { bmp = loadBitmap(uri, ctx) } catch (_: Throwable) {}; loading = false }
        else loading = false
    }

    LaunchedEffect(Unit) {
        if (editIndex >= 0) {
            val list = ImageDataManager.getImageList(ctx)
            if (editIndex < list.size && list[editIndex].filePath.isNotEmpty()) {
                try { bmp = loadBitmapFromFile(list[editIndex].filePath) } catch (_: Throwable) {}
                loading = false
            }
        }
        if (bmp == null) { loading = false; picker.launch("image/*") }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("加载中...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            return@Box
        }
        if (bmp == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                Text("未选择图片", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { picker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) {
                    Icon(painter = painterResource(android.R.drawable.ic_input_add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp)); Text("选择图片")
                }
            }
            return@Box
        }

        val bitmap = bmp!!
        val fitScale = minOf(bw / bitmap.width.toFloat(), bh / bitmap.height.toFloat())

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = { if (!processing) onFinish() }) {
                        Icon(painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                    }
                    Text(if (editIndex >= 0) "编辑兽牌" else "新建兽牌", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RectangleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize(0.75f).aspectRatio(sw / sh).clip(RectangleShape).background(Color.Black)
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
                    OutlinedButton(onClick = { if (editIndex >= 0) onFinish() else picker.launch("image/*") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = !processing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))) { Text(if (editIndex >= 0) "取消" else "重新选择", fontSize = 15.sp) }
                    Button(onClick = {
                        if (processing) return@Button
                        if (bmp != null && bw > 0 && bh > 0) {
                            processing = true
                            scope.launch {
                                try {
                                    val ts = fitScale * scale; val cx = bw / 2f; val cy = bh / 2f
                                    val cl = (bw - bitmap.width) / 2f; val ct = (bh - bitmap.height) / 2f
                                    val rl = cl * ts + cx * (1f - ts) + ox; val rt = ct * ts + cy * (1f - ts) + oy
                                    val rw = bitmap.width * ts; val rh = bitmap.height * ts
                                    val il = maxOf(0f, rl); val it2 = maxOf(0f, rt)
                                    val ir = minOf(bw, rl + rw); val ib = minOf(bh, rt + rh)
                                    val sl = ((il - cx - ox) / ts + cx - cl).toInt(); val st = ((it2 - cy - oy) / ts + cy - ct).toInt()
                                    val sr = ((ir - cx - ox) / ts + cx - cl).toInt(); val sb = ((ib - cy - oy) / ts + cy - ct).toInt()
                                    val fl = sl.coerceIn(0, bitmap.width - 1); val ft = st.coerceIn(0, bitmap.height - 1)
                                    val fw = (sr - sl).coerceAtLeast(1).coerceAtMost(bitmap.width - fl)
                                    val fh = (sb - st).coerceAtLeast(1).coerceAtMost(bitmap.height - ft)
                                    var saved: String? = null
                                    if (fw > 0 && fh > 0) {
                                        saved = withContext(Dispatchers.IO) {
                                            val cropped = Bitmap.createBitmap(bitmap, fl, ft, fw, fh)
                                            val dir = java.io.File(ctx.filesDir, "images").also { it.mkdirs() }
                                            val f = java.io.File(dir, "img_${System.currentTimeMillis()}.png")
                                            java.io.FileOutputStream(f).use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                            cropped.recycle(); f.absolutePath
                                        }
                                    }
                                    if (saved != null) {
                                        if (editIndex >= 0) ImageDataManager.replaceImage(ctx, editIndex, saved)
                                        else ImageDataManager.addImageToList(ctx, saved)
                                    }
                                } catch (e: Throwable) { android.util.Log.e("CropActivity", "Crop failed", e) }
                                finally { onFinish() }
                            }
                        } else onFinish()
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = !processing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("确认裁剪", fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
        if (processing) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp)); Text("处理中...", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

fun loadBitmapFromFile(path: String): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val dec = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        try { BitmapFactory.decodeFile(path, dec) } catch (_: Throwable) {
            dec.inSampleSize *= 2; try { BitmapFactory.decodeFile(path, dec) } catch (_: Throwable) { null }
        }
    } catch (_: Throwable) { null }
}
