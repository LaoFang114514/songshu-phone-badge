package com.laofang.songshushoupai.songshu.start.egg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.MainActivity
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.core.ImageDataManager
import com.laofang.songshushoupai.songshu.core.LocaleHelper
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiAutoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SourceLockedOrientationActivity")
class HeijiaoshouActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            SongshushoupaiAutoTheme { HeijiaoshouScreen() }
        }
    }
}

private enum class EggPhase { Jumping, Triggered }

@Composable
fun HeijiaoshouScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(EggPhase.Jumping) }
    var taps by remember { mutableIntStateOf(0) }
    val triggerAt = remember { Random.nextInt(5, 8) }
    val jumpY = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (phase == EggPhase.Jumping) {
                    taps++
                    scope.launch {
                        jumpY.animateTo(-120f, tween(160, easing = FastOutLinearInEasing))
                        jumpY.animateTo(0f, tween(320, easing = LinearOutSlowInEasing))
                    }
                    if (taps >= triggerAt) phase = EggPhase.Triggered
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (phase == EggPhase.Jumping) {
            Image(
                painter = painterResource(R.drawable.heijiaoshou),
                contentDescription = null,
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = jumpY.value.dp)
            )
        }
    }

    if (phase == EggPhase.Triggered) {
        TriggeredMusicEffect(ctx)
    }
}

@Composable
private fun TriggeredMusicEffect(ctx: Context) {
    var finished by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer.create(ctx, R.raw.tf) }

    DisposableEffect(player) {
        onDispose {
            try {
                player?.takeIf { it.isPlaying }?.stop()
            } catch (_: Throwable) {
            }
            player?.release()
        }
    }

    LaunchedEffect(Unit) {
        if (player == null) {
            finished = true
            return@LaunchedEffect
        }
        player.setOnCompletionListener { finished = true }
        try {
            player.start()
        } catch (_: Throwable) {
            finished = true
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            delay(500.milliseconds)
            ensureShilieggBadge(ctx)
            restartApp(ctx)
        }
    }
}

private fun ensureShilieggBadge(ctx: Context) {
    try {
        val dir = File(ctx.filesDir, "images").also { it.mkdirs() }
        val target = File(dir, "egg_shiliegg.png")
        val path = target.absolutePath
        val list = ImageDataManager.getImageList(ctx)
        if (list.any { it.filePath == path }) return
        val bmp = BitmapFactory.decodeResource(ctx.resources, R.drawable.shiliegg) ?: return
        target.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        bmp.recycle()
        ImageDataManager.addImageToList(ctx, path)
        val idx = ImageDataManager.getImageList(ctx).indexOfFirst { it.filePath == path }
        if (idx >= 0) {
            val name = LocaleHelper.applyLocale(ctx).getString(R.string.heijiaoshou_badge_name)
            ImageDataManager.renameItem(ctx, idx, name)
        }
    } catch (_: Throwable) {
    }
}
private fun restartApp(ctx: Context) {
    val intent = Intent(ctx, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    ctx.startActivity(intent)
    (ctx as? Activity)?.finish()
}
