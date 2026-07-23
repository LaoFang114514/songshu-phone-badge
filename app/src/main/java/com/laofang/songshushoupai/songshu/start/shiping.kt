@file:OptIn(UnstableApi::class)
@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package com.laofang.songshushoupai.songshu.start

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.view.WindowManager
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiAutoTheme
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.util.UnstableApi
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File

class VideoPlayerActivity : ComponentActivity() {
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
    val screenAR = dm.widthPixels.toFloat() / dm.heightPixels.toFloat()

    val antiBurnInOffset = rememberAntiBurnInOffset(s.antiBurnIn, dm.heightPixels.toFloat())
    var showInitialHint by remember { mutableStateOf(true) }
    var hideHintJob by remember { mutableStateOf<Job?>(null) }
    var showRotationHint by remember { mutableStateOf(false) }
    val batteryLevel = rememberBatteryLevel()
    var videoAspectRatio by remember { mutableFloatStateOf(0f) }
    var gestureScale by remember { mutableFloatStateOf(1f) }
    var gestureResetJob by remember { mutableStateOf<Job?>(null) }
    var showQrOverlay by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val qr = rememberQrAnim(showQrOverlay)

    LaunchedEffect(Unit) {
        if (s.defaultOrientation) rotation = 180f
        hideHintJob = launch { delay(2000.milliseconds); if (isActive) showInitialHint = false }
        if (s.showQrCode) {
            qrBitmap = withContext(Dispatchers.IO) {
                val p = s.qrCodePath
                if (p.isNotEmpty() && File(p).exists())
                    try { BitmapFactory.decodeFile(p) } catch (_: Throwable) { null }
                else try { BitmapFactory.decodeResource(context.resources, R.drawable.qr_zanzhu) } catch (_: Throwable) { null }
            }
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoPath))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare(); playWhenReady = true
            volume = if (s.muteVideo) 0f else 1f
        }
    }
    var playerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                            val sf = Surface(surface); setTag(R.id.tag_surface, sf); playerRef?.setVideoSurface(sf)
                        }
                        override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean {
                            (getTag(R.id.tag_surface) as? Surface)?.release(); setTag(R.id.tag_surface, null)
                            playerRef?.setVideoSurface(null); return true
                        }
                        override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                    }
                }
            },
            update = { view ->
                playerRef = exoPlayer
                if (view.isAvailable && view.surfaceTexture != null && view.getTag(R.id.tag_surface) == null) {
                    val sf = Surface(view.surfaceTexture); view.setTag(R.id.tag_surface, sf); exoPlayer.setVideoSurface(sf)
                }
                val tg = exoPlayer.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
                if (tg != null) for (i in 0 until tg.length) {
                    if (tg.isTrackSupported(i)) {
                        val f = tg.getTrackFormat(i)
                        if (f.width > 0 && f.height > 0) { videoAspectRatio = f.width.toFloat() / f.height; break }
                    }
                }
            },
            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                .aspectRatio(if (videoAspectRatio > 0f) videoAspectRatio else screenAR)
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

        BatteryOverlay(batteryLevel, rotation)
        InitialHintOverlay(s.lockOrientation, rotation, showInitialHint)
        RotationHintOverlay(rotation, showRotationHint)
        QrOverlay(qr, qrBitmap, rotation) { showQrOverlay = false }
    }
}

