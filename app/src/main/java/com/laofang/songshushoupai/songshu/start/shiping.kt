@file:OptIn(UnstableApi::class)
@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package com.laofang.songshushoupai.songshu.start

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.laofang.songshushoupai.songshu.core.LocaleHelper
import com.laofang.songshushoupai.songshu.core.QrCodeDataManager
import com.laofang.songshushoupai.songshu.core.SettingsManager
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiAutoTheme
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.util.UnstableApi

class VideoPlayerActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        val settings = SettingsManager.loadSettings(this)
        if (settings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val videoPath = intent.getStringExtra("videoPath") ?: ""
        setContent { SongshushoupaiAutoTheme { FullScreenVideoScreen(videoPath) } }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("LocalContextResourcesRead")
@Composable
fun FullScreenVideoScreen(videoPath: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val s = remember { SettingsManager.loadSettings(context) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val dm = context.resources.displayMetrics

    val antiBurnInOffset = rememberAntiBurnInOffset(s.antiBurnIn, dm.heightPixels.toFloat())
    var showInitialHint by remember { mutableStateOf(true) }
    var hideHintJob by remember { mutableStateOf<Job?>(null) }
    var showRotationHint by remember { mutableStateOf(false) }
    val batteryLevel = rememberBatteryLevel()
    var gestureScale by remember { mutableFloatStateOf(1f) }
    var gestureResetJob by remember { mutableStateOf<Job?>(null) }
    var showQrOverlay by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(0f) }
    val qrOverlay = rememberQrOverlay(s.showQrCode)
    val qrList = remember { QrCodeDataManager.getQrList(context) }

    LaunchedEffect(Unit) {
        if (s.defaultOrientation) rotation = 180f
        hideHintJob = launch { delay(2000.milliseconds); if (isActive) showInitialHint = false }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoPath))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare(); playWhenReady = true
            volume = if (s.muteVideo) 0f else 1f
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val w = videoSize.width
                val h = videoSize.height
                if (w > 0 && h > 0) {
                    val rot = videoSize.unappliedRotationDegrees
                    videoAspectRatio = if (rot == 90 || rot == 270) {
                        h.toFloat() / w.toFloat()
                    } else {
                        (w.toFloat() * videoSize.pixelWidthHeightRatio) / h.toFloat()
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        if (exoPlayer.videoSize.width > 0) listener.onVideoSizeChanged(exoPlayer.videoSize)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                AspectRatioFrameLayout(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    val textureView = TextureView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                            exoPlayer.setVideoTextureView(textureView)
                        }
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            exoPlayer.setVideoTextureView(null)
                            return true
                        }
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                    addView(textureView)
                }
            },
            update = { frame: AspectRatioFrameLayout ->
                if (videoAspectRatio > 0f) frame.setAspectRatio(videoAspectRatio)
            },
            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                .graphicsLayer { rotationZ = rotation; translationY = antiBurnInOffset.value }
        )

        Box(Modifier.fillMaxSize().fullScreenGestures(
            scope = scope,
            showQrCode = s.showQrCode,
            rotation = rotation,
            lockOrientation = s.lockOrientation,
            onRotationToggle = {
                rotation = if (rotation == 0f) 180f else 0f
                showRotationHint = true
            },
            showRotationHint = { showRotationHint = it },
            gestures = GestureCallbacks(
                onSwipeDown = { showQrOverlay = true },
                onSwipeUp = { showQrOverlay = true },
                onTap = {
                    hideHintJob?.cancel(); showInitialHint = true
                    hideHintJob = scope.launch { delay(2000.milliseconds); if (isActive) showInitialHint = false }
                }
            ),
            onPinchScale = { delta ->
                gestureResetJob?.cancel()
                gestureScale = if (gestureScale == 1f) delta else gestureScale * delta
                if (gestureScale < 0.7f) {
                    gestureScale = 1f
                    scope.launch {
                        delay(100.milliseconds)
                        if (isActive) try { activity?.let { if (!it.isFinishing && !it.isDestroyed) { it.finish(); it.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) } } } catch (_: Exception) {}
                    }
                }
                gestureResetJob = scope.launch { delay(500.milliseconds); gestureScale = 1f }
            }
        ))

        BatteryOverlay(batteryLevel, rotation, s.showBattery)
        InitialHintOverlay(s.lockOrientation, rotation, showInitialHint)
        RotationHintOverlay(rotation, showRotationHint)
        QrOverlay(rememberQrAnim(showQrOverlay), qrOverlay.qrBitmap, rotation, s.qrSwipeSwitch, qrList.size, { }) { showQrOverlay = false }
    }
}

