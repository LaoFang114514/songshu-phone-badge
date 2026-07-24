package com.laofang.songshushoupai.songshu.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.laofang.songshushoupai.songshu.BuildConfig
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.start.egg.ColorSudokuActivity

private val BtnShape = RoundedCornerShape(12.dp)

@Composable
fun AboutSettingsCard() {
    val ctx = LocalContext.current
    var clicks by remember { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).clickable {
            clicks++
            if (clicks >= 7) {
                clicks = 0
                try {
                    (ctx as? Activity)?.startActivity(Intent(ctx, ColorSudokuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        ?: Toast.makeText(ctx, "无法访问页面", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "跳转失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    android.widget.ImageView(context).apply {
                        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.qidong))
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("松鼠兽牌", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(6.dp), color = cs.primary.copy(alpha = 0.12f)) {
            Text("V${BuildConfig.VERSION_NAME}", Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium, color = cs.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("一款简单易用的旧手机变兽牌工具，帮助你使用旧手机快速创建和展示自己的兽牌图片，适合需要兽牌但经费不足的小伙伴。",
            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkBtn("QQ群", "https://qun.qq.com/universal-pop/pop.html?ucid=JxRTxJmIQfa8p4d0_U_TyZyEn&gc=&sign=dc86ae0ca4700c5dbc23894f0fdb82fccfd937cd0fe5edec4afa9472c7300d07&external=&_type=gp&o&_client=yqq&hash=-")
            LinkBtn("GitHub", "https://github.com/LaoFang114514/songshu-phone-badge")
            LinkBtn("官网", "https://songshushoupai.mysxl.cn/")
        }
    }
}

@Composable
private fun LinkBtn(label: String, url: String) {
    val ctx = LocalContext.current
    OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
        Modifier.fillMaxWidth(), shape = BtnShape) { Text(label) }
}
