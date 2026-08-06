package com.laofang.songshushoupai.songshu.start

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laofang.songshushoupai.songshu.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import kotlin.math.sqrt
import java.io.File
import com.laofang.songshushoupai.songshu.core.QrCodeDataManager

data class GestureCallbacks(
    val onSwipeDown: () -> Unit,
    val onSwipeUp: () -> Unit,
    val onTap: () -> Unit
)

@Composable
fun Modifier.fullScreenGestures(
    scope: CoroutineScope,
    showQrCode: Boolean,
    rotation: Float,
    lockOrientation: Boolean,
    onRotationToggle: () -> Unit,
    showRotationHint: (Boolean) -> Unit,
    gestures: GestureCallbacks,
    onPinchScale: ((Float) -> Unit)? = null
): Modifier = this.pointerInput(rotation, showQrCode) {
    awaitEachGesture {
        val firstDown = awaitFirstDown()
        val startX = firstDown.position.x; val startY = firstDown.position.y
        var isMultiTouch = false; var isLongPress = false; var moved = false
        var pendingSwipeDown = false; var pendingSwipeUp = false; var wasMultiTouch = false

        val longPressJob = scope.launch {
            delay(300L.milliseconds)
            if (!moved && !isMultiTouch) {
                isLongPress = true
                if (!lockOrientation) {
                    onRotationToggle()
                    showRotationHint(true)
                    scope.launch { delay(2000.milliseconds); if (isActive) showRotationHint(false) }
                }
            }
        }

        var lastDist = 0f
        while (true) {
            val event = awaitPointerEvent()
            val changes = event.changes.filter { it.pressed }
            if (changes.isEmpty()) break
            if (changes.size >= 2) {
                if (!isMultiTouch) { isMultiTouch = true; wasMultiTouch = true; pendingSwipeDown = false; pendingSwipeUp = false }
                longPressJob.cancel()
                if (onPinchScale != null) {
                    val p1 = changes[0].position; val p2 = changes[1].position
                    val currentDist = sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
                    if (lastDist > 0f) onPinchScale(currentDist / lastDist)
                    lastDist = currentDist
                }
                changes.forEach { it.consume() }
            } else {
                if (wasMultiTouch) { changes.first().consume(); continue }
                val change = changes.first()
                val totalDy = change.position.y - startY; val totalDx = change.position.x - startX
                val dragDist = sqrt((change.position.x - firstDown.position.x) * (change.position.x - firstDown.position.x) + (change.position.y - firstDown.position.y) * (change.position.y - firstDown.position.y))
                if (!pendingSwipeDown && !pendingSwipeUp && totalDy > 150f && abs(totalDy) > abs(totalDx) * 1.5f) { pendingSwipeDown = true; longPressJob.cancel() }
                if (!pendingSwipeDown && !pendingSwipeUp && totalDy < -150f && abs(totalDy) > abs(totalDx) * 1.5f) { pendingSwipeUp = true; longPressJob.cancel() }
                if (dragDist > 20f) { moved = true; longPressJob.cancel() }
                change.consume()
            }
        }
        longPressJob.cancel()
        val sd = pendingSwipeDown; val su = pendingSwipeUp
        if (showQrCode && ((sd && rotation == 0f) || (su && rotation == 180f))) gestures.onSwipeDown()
        else if (!isMultiTouch && !isLongPress && !moved) gestures.onTap()
    }
}

@Composable
fun rememberBatteryLevel(): Int {
    val ctx = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level = if (l >= 0 && s > 0) l * 100 / s else -1
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ctx.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED)
        else ctx.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { ctx.unregisterReceiver(r) }
    }
    return level
}

@Composable
fun rememberAntiBurnInOffset(enabled: Boolean, screenH: Float): Animatable<Float, *> {
    val o = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) { o.snapTo(0f); return@LaunchedEffect }
        val s3 = screenH * 0.03f; val s6 = screenH * 0.06f
        while (isActive) {
            delay(300000L.milliseconds); o.animateTo(-s3, tween(3000))
            delay(300000L.milliseconds); o.animateTo(-s3 + s6, tween(3000))
            delay(300000L.milliseconds); o.animateTo(0f, tween(3000))
        }
    }
    return o
}

data class QrAnims(
    val inComp: Boolean,
    val bgAlpha: Animatable<Float, *>,
    val cScale: Animatable<Float, *>,
    val cAlpha: Animatable<Float, *>
)

@Composable
fun rememberQrAnim(show: Boolean): QrAnims {
    var ic by remember { mutableStateOf(false) }
    val ba = remember { Animatable(0f) }
    val cs = remember { Animatable(0.6f) }
    val ca = remember { Animatable(0f) }
    LaunchedEffect(show) {
        if (show) {
            ic = true; ba.snapTo(0f); cs.snapTo(0.6f); ca.snapTo(0f)
            launch { ba.animateTo(1f, tween(300)) }
            launch { cs.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
            launch { ca.animateTo(1f, tween(250)) }
        } else if (ic) {
            launch { ba.animateTo(0f, tween(200)) }
            launch { ca.animateTo(0f, tween(200)) }
            delay(200.milliseconds); ic = false
        }
    }
    return QrAnims(ic, ba, cs, ca)
}

data class QrOverlayState(
    val qrBitmap: Bitmap?,
    val showQrCode: Boolean
)

@SuppressLint("LocalContextResourcesRead")
@Composable
fun rememberQrOverlay(enabled: Boolean): QrOverlayState {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(enabled) {
        if (!enabled) { qrBitmap = null; return@LaunchedEffect }
        qrBitmap = withContext(Dispatchers.IO) {
            val qrList = QrCodeDataManager.getQrList(context)
            val qrSel = QrCodeDataManager.getSelectedIndex(context).coerceIn(0, (qrList.size - 1).coerceAtLeast(0))
            val path = qrList.getOrNull(qrSel)?.path ?: ""
            if (path.isNotEmpty() && File(path).exists())
                try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
            else try { BitmapFactory.decodeResource(context.resources, R.drawable.qr_zanzhu) } catch (_: Throwable) { null }
        }
    }
    return QrOverlayState(qrBitmap, enabled)
}

@Composable
fun BoxScope.BatteryOverlay(level: Int, rotation: Float, showBattery: Boolean = true) {
    if (!showBattery || level < 0) return
    Box(Modifier.align(if (rotation == 0f) Alignment.TopCenter else Alignment.BottomCenter)
        .graphicsLayer(rotationZ = rotation).padding(top = 12.dp)) {
        Text(stringResource(R.string.battery_level, level), color = Color.White, fontSize = 13.sp,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun BoxScope.InitialHintOverlay(lock: Boolean, rotation: Float, visible: Boolean) {
    if (!visible) return
    Box(Modifier.align(if (rotation == 0f) Alignment.BottomCenter else Alignment.TopCenter)
        .graphicsLayer(rotationZ = rotation)) {
        Text(
            if (lock) stringResource(R.string.pinch_to_exit) else stringResource(R.string.pinch_exit_rotate),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 50.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun BoxScope.RotationHintOverlay(rotation: Float, visible: Boolean) {
    if (!visible) return
    Box(Modifier.align(Alignment.Center).graphicsLayer(rotationZ = rotation)) {
        Text(stringResource(R.string.badge_flipped), color = Color.White,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun QrOverlay(qr: QrAnims, qrBitmap: Bitmap?, rotation: Float, qrSwipeSwitch: Boolean, qrCount: Int, onQrIndexChange: (Int) -> Unit, onTap: () -> Unit) {
    if (!qr.inComp) return
    val context = LocalContext.current
    var currentIndex by remember(qrBitmap) { mutableIntStateOf(QrCodeDataManager.getSelectedIndex(context).coerceIn(0, (qrCount - 1).coerceAtLeast(0))) }
    var currentBitmap by remember(currentIndex) { mutableStateOf<Bitmap?>(null) }
    val slideAnim = remember { Animatable(0f) }
    var slideDirection by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIndex) {
        currentBitmap = withContext(Dispatchers.IO) {
            val qrList = QrCodeDataManager.getQrList(context)
            if (qrList.isEmpty()) return@withContext qrBitmap
            val idx = currentIndex.coerceIn(0, (qrList.size - 1).coerceAtLeast(0))
            val path = qrList.getOrNull(idx)?.path ?: ""
            if (path.isNotEmpty() && File(path).exists())
                try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null }
            else try { BitmapFactory.decodeResource(context.resources, R.drawable.qr_zanzhu) } catch (_: Throwable) { null }
        }
    }

    LaunchedEffect(qrBitmap) {
        currentIndex = QrCodeDataManager.getSelectedIndex(context).coerceIn(0, (qrCount - 1).coerceAtLeast(0))
        currentBitmap = qrBitmap
        slideAnim.snapTo(0f)
        slideDirection = 0f
    }

    LaunchedEffect(slideDirection) {
        if (slideDirection != 0f) {
            slideAnim.snapTo(slideDirection)
            slideAnim.animateTo(0f, tween(300))
            slideDirection = 0f
        }
    }

    Box(Modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = 0.85f * qr.bgAlpha.value))
        .graphicsLayer(rotationZ = rotation)
        .pointerInput(qrSwipeSwitch, qrCount) {
            if (qrSwipeSwitch && qrCount > 1) {
                var startX: Float
                awaitEachGesture {
                    val down = awaitFirstDown()
                    startX = down.position.x
                    var moved = false
                    var endX = startX
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes.filter { it.pressed }
                        if (changes.isEmpty()) break
                        val change = changes.first()
                        endX = change.position.x
                        if (abs(endX - startX) > 20f) moved = true
                        change.consume()
                    }
                    val dx = endX - startX
                    if (moved && abs(dx) > 80f) {
                        val newIndex = if (dx < 0) {
                            (currentIndex + 1) % qrCount
                        } else {
                            (currentIndex - 1 + qrCount) % qrCount
                        }
                        val dir = if (dx < 0) 1f else -1f
                        currentIndex = newIndex
                        QrCodeDataManager.setSelectedIndex(context, newIndex)
                        onQrIndexChange(newIndex)
                        slideDirection = dir * 0.35f
                    } else if (!moved) {
                        onTap()
                    }
                }
            } else {
                detectTapGestures(onTap = { onTap() })
            }
        },
        contentAlignment = Alignment.Center) {
        val progress = slideAnim.value
        val absProgress = abs(progress)
        val showTransition = absProgress > 0.01f

        if (showTransition) {
            Image(
                painter = BitmapPainter((currentBitmap ?: qrBitmap)!!.asImageBitmap()),
                contentDescription = stringResource(R.string.qr_code),
                modifier = Modifier.fillMaxSize(0.85f).graphicsLayer {
                    translationX = -progress * size.width
                    scaleX = qr.cScale.value; scaleY = qr.cScale.value; alpha = qr.cAlpha.value
                },
                contentScale = ContentScale.Fit)
        } else {
            val bmp = currentBitmap ?: qrBitmap
            if (bmp != null) Image(
                painter = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = stringResource(R.string.qr_code),
                modifier = Modifier.fillMaxSize(0.85f).graphicsLayer {
                    scaleX = qr.cScale.value; scaleY = qr.cScale.value; alpha = qr.cAlpha.value
                },
                contentScale = ContentScale.Fit)
            else Text(stringResource(R.string.qr_loading), color = Color.White.copy(alpha = qr.cAlpha.value))
        }
    }
}
