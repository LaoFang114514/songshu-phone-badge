package com.laofang.songshushoupai.songshu.start

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiAutoTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.laofang.songshushoupai.songshu.ImageDataManager
import com.laofang.songshushoupai.songshu.SettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class StartActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            SongshushoupaiAutoTheme { FullScreenImageScreen() }
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
    var rotation by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val screenHeightPx = context.resources.displayMetrics.heightPixels.toFloat()

    val antiBurnInOffset = rememberAntiBurnInOffset(appSettings.antiBurnIn, screenHeightPx)

    var showInitialHint by remember { mutableStateOf(true) }
    var hideInitialHintJob by remember { mutableStateOf<Job?>(null) }

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val batteryLevel = rememberBatteryLevel()

    var showQrOverlay by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val qr = rememberQrAnim(showQrOverlay)

    val exitAlpha = remember { Animatable(1f) }
    var gestureScale by remember { mutableFloatStateOf(1f) }
    var gestureResetJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (appSettings.defaultOrientation) rotation = 180f

        val imagePath = ImageDataManager.getCurrentImagePath(context)
        loadedBitmap = if (imagePath.isNotEmpty()) {
            try {
                val opts = BitmapFactory.Options()
                opts.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imagePath, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) null
                else {
                    opts.inJustDecodeBounds = false
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888
                    BitmapFactory.decodeFile(imagePath, opts)
                }
            } catch (_: Throwable) { null }
        } else null
        isLoaded = true

        if (appSettings.showQrCode) {
            qrBitmap = withContext(Dispatchers.IO) {
                val path = appSettings.qrCodePath
                if (path.isNotEmpty() && File(path).exists())
                    try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
                else try {
                    BitmapFactory.decodeResource(context.resources, com.laofang.songshushoupai.songshu.R.drawable.qr_zanzhu)
                } catch (_: Throwable) { null }
            }
        }

        hideInitialHintJob = launch { delay(2000.milliseconds); if (isActive) showInitialHint = false }
    }

    if (!isLoaded) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer { alpha = exitAlpha.value }) {
            val bitmap = loadedBitmap
            if (bitmap != null) {
                Image(BitmapPainter(bitmap.asImageBitmap()), "全屏显示图片",
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation; translationY = antiBurnInOffset.value },
                    contentScale = ContentScale.Fit)
            } else {
                Image(painterResource(id = com.laofang.songshushoupai.songshu.R.drawable.shili), "全屏显示图片",
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation; translationY = antiBurnInOffset.value },
                    contentScale = ContentScale.Fit)
            }

            var showRotationHint by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize().fullScreenGestures(
                scope = coroutineScope,
                showQrCode = appSettings.showQrCode,
                rotation = rotation,
                lockOrientation = appSettings.lockOrientation,
                onRotationToggle = {
                    rotation = if (rotation == 0f) 180f else 0f
                    showRotationHint = true
                },
                showRotationHint = { showRotationHint = it },
                gestures = GestureCallbacks(
                    onSwipeDown = { showQrOverlay = true },
                    onSwipeUp = { showQrOverlay = true },
                    onTap = {
                        hideInitialHintJob?.cancel(); showInitialHint = true
                        hideInitialHintJob = coroutineScope.launch { delay(2000.milliseconds); if (isActive) showInitialHint = false }
                    }
                ),
                onPinchScale = { delta ->
                    gestureResetJob?.cancel()
                    val newScale = (gestureScale * delta).coerceIn(0.3f, 3f)
                    if (newScale < 0.5f && gestureScale >= 0.5f) {
                        gestureScale = 1f
                        try { activity?.let { if (!it.isFinishing && !it.isDestroyed) { it.finish(); it.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) } } } catch (_: Exception) {}
                    } else {
                        gestureScale = newScale
                    }
                    gestureResetJob = coroutineScope.launch { delay(500.milliseconds); gestureScale = 1f }
                }
            ))

            BatteryOverlay(batteryLevel, rotation)
            InitialHintOverlay(appSettings.lockOrientation, rotation, showInitialHint)
            RotationHintOverlay(rotation, showRotationHint)
            QrOverlay(qr, qrBitmap, rotation) { showQrOverlay = false }
        }
}
