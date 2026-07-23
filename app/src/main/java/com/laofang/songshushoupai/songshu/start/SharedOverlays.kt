package com.laofang.songshushoupai.songshu.start

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs
import kotlin.math.sqrt

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

@Composable
fun BoxScope.BatteryOverlay(level: Int, rotation: Float) {
    if (level < 0) return
    Box(Modifier.align(if (rotation == 0f) Alignment.TopCenter else Alignment.BottomCenter)
        .graphicsLayer(rotationZ = rotation).padding(top = 12.dp)) {
        Text("电量：$level%", color = Color.White, fontSize = 13.sp,
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
            if (lock) "双指捏合退出播放" else "双指捏合退出播放\n长按屏幕旋转兽牌",
            color = Color.White,
            modifier = Modifier.padding(bottom = 50.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun BoxScope.RotationHintOverlay(rotation: Float, visible: Boolean) {
    if (!visible) return
    Box(Modifier.align(Alignment.Center).graphicsLayer(rotationZ = rotation)) {
        Text("已翻转兽牌", color = Color.White,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun QrOverlay(qr: QrAnims, qrBitmap: Bitmap?, rotation: Float, onTap: () -> Unit) {
    if (!qr.inComp) return
    Box(Modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = 0.85f * qr.bgAlpha.value))
        .graphicsLayer(rotationZ = rotation)
        .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center) {
        if (qrBitmap != null) Image(
            painter = BitmapPainter(qrBitmap.asImageBitmap()),
            contentDescription = "二维码",
            modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 400.dp).graphicsLayer {
                scaleX = qr.cScale.value; scaleY = qr.cScale.value; alpha = qr.cAlpha.value
            },
            contentScale = ContentScale.Fit)
        else Text("二维码加载中...", color = Color.White.copy(alpha = qr.cAlpha.value))
    }
}
