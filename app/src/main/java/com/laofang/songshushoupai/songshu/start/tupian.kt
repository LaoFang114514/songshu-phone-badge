package com.laofang.songshushoupai.songshu.start

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import com.laofang.songshushoupai.songshu.ImageDataManager
import com.laofang.songshushoupai.songshu.SettingsManager
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiTheme
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.milliseconds
import java.io.File

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val settings = SettingsManager.loadSettings(this)
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val list = ImageDataManager.getImageList(this)
        val idx = ImageDataManager.getSelectedIndex(this).coerceIn(0, (list.size - 1).coerceAtLeast(0))
        val currentItem = list.getOrNull(idx)

        if (currentItem != null && currentItem.isVideo) {
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("videoPath", currentItem.filePath)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            val context = LocalContext.current
            val currentSettings = remember { SettingsManager.loadSettings(context) }
            val useDarkTheme = when (currentSettings.darkMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            SongshushoupaiTheme(
                darkTheme = useDarkTheme,
                themeColorIndex = currentSettings.themeColorIndex
            ) {
                FullScreenImageScreen()
            }
        }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("LocalContextResourcesRead")
@Composable
fun FullScreenImageScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val appSettings = remember { SettingsManager.loadSettings(context) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val antiBurnInOffset = remember { Animatable(0f) }
    val screenHeightPx = context.resources.displayMetrics.heightPixels.toFloat()

    var showInitialHint by remember { mutableStateOf(true) }
    var hideInitialHintJob by remember { mutableStateOf<Job?>(null) }

    var showRotationHint by remember { mutableStateOf(false) }
    var hideRotationHintJob by remember { mutableStateOf<Job?>(null) }

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    var batteryLevel by remember { mutableIntStateOf(-1) }

    var showQrOverlay by remember { mutableStateOf(false) }
    var qrInComposition by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val qrBgAlpha = remember { Animatable(0f) }
    val qrContentScale = remember { Animatable(0.6f) }
    val qrContentAlpha = remember { Animatable(0f) }

    LaunchedEffect(showQrOverlay) {
        if (showQrOverlay) {
            qrInComposition = true
            qrBgAlpha.snapTo(0f)
            qrContentScale.snapTo(0.6f)
            qrContentAlpha.snapTo(0f)
            launch { qrBgAlpha.animateTo(1f, tween(300)) }
            launch { qrContentScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
            launch { qrContentAlpha.animateTo(1f, tween(250)) }
        } else if (qrInComposition) {
            launch { qrBgAlpha.animateTo(0f, tween(200)) }
            launch { qrContentAlpha.animateTo(0f, tween(200)) }
            delay(200.milliseconds)
            qrInComposition = false
        }
    }

    LaunchedEffect(Unit) {
        if (appSettings.defaultOrientation) {
            rotation = 180f
        }
    }

    // 监听电池电量变化
    DisposableEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = if (level >= 0 && scale > 0) {
                    level * 100 / scale
                } else -1
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        val imagePath = ImageDataManager.getCurrentImagePath(context)
        loadedBitmap = if (imagePath.isNotEmpty()) {
            try {
                val opts = BitmapFactory.Options()
                opts.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imagePath, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                    null
                } else {
                    opts.inJustDecodeBounds = false
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888
                    BitmapFactory.decodeFile(imagePath, opts)
                }
            } catch (_: Throwable) { null }
        } else null
        isLoaded = true

        if (appSettings.showQrCode) {
            qrBitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                val path = appSettings.qrCodePath
                if (path.isNotEmpty() && File(path).exists()) {
                    try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
                } else {
                    try {
                        BitmapFactory.decodeResource(context.resources, com.laofang.songshushoupai.songshu.R.drawable.qr_zanzhu)
                    } catch (_: Throwable) { null }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        hideInitialHintJob = launch {
            delay(2000.milliseconds)
            if (isActive) {
                showInitialHint = false
            }
        }
    }

    LaunchedEffect(appSettings.antiBurnIn) {
        if (!appSettings.antiBurnIn) {
            antiBurnInOffset.snapTo(0f)
            return@LaunchedEffect
        }
        val step3 = screenHeightPx * 0.03f
        val step6 = screenHeightPx * 0.06f
        while (isActive) {
            delay(300000L.milliseconds)
            antiBurnInOffset.animateTo(-step3, tween(3000))
            delay(300000L.milliseconds)
            antiBurnInOffset.animateTo(-step3 + step6, tween(3000))
            delay(300000L.milliseconds)
            antiBurnInOffset.animateTo(0f, tween(3000))
        }
    }

    if (!isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val bitmap = loadedBitmap
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = "全屏显示图片",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        translationY = antiBurnInOffset.value
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = com.laofang.songshushoupai.songshu.R.drawable.shili),
                contentDescription = "全屏显示图片",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        translationY = antiBurnInOffset.value
                    },
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown()
                        val startX = firstDown.position.x
                        val startY = firstDown.position.y
                        var isMultiTouch = false
                        var isLongPress = false
                        var moved = false
                        var pendingSwipeDown = false
                        var pendingSwipeUp = false
                        var wasMultiTouch = false

                        val longPressJob = coroutineScope.launch {
                            delay(300L.milliseconds)
                            if (!moved && !isMultiTouch) {
                                isLongPress = true
                                if (!appSettings.lockOrientation) {
                                    rotation = if (rotation == 0f) 180f else 0f
                                    hideRotationHintJob?.cancel()
                                    showRotationHint = true
                                    hideRotationHintJob = coroutineScope.launch {
                                        delay(2000.milliseconds)
                                        if (isActive) {
                                            showRotationHint = false
                                        }
                                    }
                                }
                            }
                        }

                        var lastZoom = 1f
                        var lastPanX = 0f
                        var lastPanY = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes.filter { it.pressed }
                            if (changes.isEmpty()) break

                            if (changes.size >= 2) {
                                if (!isMultiTouch) {
                                    isMultiTouch = true
                                    wasMultiTouch = true
                                    pendingSwipeDown = false
                                    pendingSwipeUp = false
                                }
                                longPressJob.cancel()

                                val p1 = changes[0].position
                                val p2 = changes[1].position
                                val currentDist = kotlin.math.sqrt(
                                    (p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)
                                )
                                val center = androidx.compose.ui.geometry.Offset(
                                    (p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f
                                )

                                if (lastZoom == 1f && lastPanX == 0f && lastPanY == 0f) {
                                    // first multi-touch frame
                                } else {
                                    val zoomDelta = if (lastZoom > 0f) currentDist / lastZoom else 1f
                                    val newScale = (scale * zoomDelta).coerceIn(0.3f, 3f)
                                    if (newScale < 0.5f && scale >= 0.5f) {
                                        coroutineScope.launch {
                                            delay(100.milliseconds)
                                            if (isActive) {
                                                try {
                                                    activity?.let {
                                                        if (!it.isFinishing && !it.isDestroyed) {
                                                            it.finish()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    }
                                    scale = newScale
                                    if (scale > 1f) {
                                        offsetX += center.x - lastPanX
                                        offsetY += center.y - lastPanY
                                    }
                                }
                                lastZoom = currentDist
                                lastPanX = center.x
                                lastPanY = center.y

                                changes.forEach { it.consume() }
                            } else {
                                if (wasMultiTouch) {
                                    changes.first().consume()
                                    continue
                                }

                                val change = changes.first()
                                val totalDy = change.position.y - startY
                                val totalDx = change.position.x - startX
                                val dragDist = kotlin.math.sqrt(
                                    (change.position.x - firstDown.position.x) * (change.position.x - firstDown.position.x) +
                                    (change.position.y - firstDown.position.y) * (change.position.y - firstDown.position.y)
                                )

                                if (!pendingSwipeDown && !pendingSwipeUp && totalDy > 150f && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) * 1.5f) {
                                    pendingSwipeDown = true
                                    longPressJob.cancel()
                                }

                                if (!pendingSwipeDown && !pendingSwipeUp && totalDy < -150f && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) * 1.5f) {
                                    pendingSwipeUp = true
                                    longPressJob.cancel()
                                }

                                if (dragDist > 20f) {
                                    moved = true
                                    longPressJob.cancel()
                                }
                                change.consume()
                            }
                        }

                        longPressJob.cancel()

                        val swipeDownConfirmed = pendingSwipeDown && !wasMultiTouch
                        val swipeUpConfirmed = pendingSwipeUp && !wasMultiTouch

                        if (appSettings.showQrCode &&
                            ((swipeDownConfirmed && rotation == 0f) || (swipeUpConfirmed && rotation == 180f))
                        ) {
                            showQrOverlay = true
                        } else if (!isMultiTouch && !isLongPress && !moved) {
                            hideInitialHintJob?.cancel()
                            showInitialHint = true
                            hideInitialHintJob = coroutineScope.launch {
                                delay(2000.milliseconds)
                                if (isActive) {
                                    showInitialHint = false
                                }
                            }
                        }
                    }
                }
        )

        // 显示电池电量
        if (appSettings.showBattery && batteryLevel >= 0) {
            Box(
                modifier = Modifier
                    .align(if (rotation == 0f) Alignment.TopCenter else Alignment.BottomCenter)
                    .graphicsLayer(rotationZ = rotation)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "电量：$batteryLevel%",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp
                )
            }
        }

        if (showInitialHint) {
            Box(
                modifier = Modifier
                    .align(if (rotation == 0f) Alignment.BottomCenter else Alignment.TopCenter)
                    .graphicsLayer(rotationZ = rotation)
            ) {
                Text(
                    text = if (appSettings.lockOrientation) "双指捏合退出播放" else "双指捏合退出播放\n长按屏幕旋转兽牌",
                    color = Color.White,
                    modifier = Modifier
                        .padding(bottom = 50.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (showRotationHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(rotationZ = rotation)
            ) {
                Text(
                    text = "已翻转兽牌",
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (qrInComposition) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f * qrBgAlpha.value))
                    .graphicsLayer(rotationZ = rotation)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showQrOverlay = false })
                    },
                contentAlignment = Alignment.Center
            ) {
                val bmp = qrBitmap
                if (bmp != null) {
                    Image(
                        painter = BitmapPainter(bmp.asImageBitmap()),
                        contentDescription = "二维码",
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(max = 400.dp)
                            .graphicsLayer {
                                scaleX = qrContentScale.value
                                scaleY = qrContentScale.value
                                alpha = qrContentAlpha.value
                            },
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("二维码加载中...", color = Color.White.copy(alpha = qrContentAlpha.value))
                }
            }
        }
    }
}
