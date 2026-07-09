package com.laofang.songshushoupai.songshu.settings

import android.app.Activity
import android.widget.Toast
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.laofang.songshushoupai.songshu.BuildConfig
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.start.egg.ColorSudokuActivity

private val BtnShape = RoundedCornerShape(12.dp)

@Composable
fun AboutSettingsCard() {
    val ctx = LocalContext.current
    var clicks by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    clicks++
                    if (clicks >= 7) {
                        clicks = 0
                        try {
                            val a = ctx as? Activity
                            if (a != null) {
                                a.startActivity(Intent(a, ColorSudokuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } else Toast.makeText(ctx, "无法访问页面", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "跳转失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.mipmap.ic_launcher_background), null, Modifier.fillMaxSize())
            Image(painterResource(R.mipmap.ic_launcher_foreground), "应用图标", Modifier.size(98.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("松鼠兽牌", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Text("V${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("一款简单易用的旧手机变兽牌工具，帮助你使用旧手机快速创建和展示自己的兽牌图片，适合需要兽牌但经费不足的小伙伴。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
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
    OutlinedButton(
        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
        modifier = Modifier.fillMaxWidth(), shape = BtnShape
    ) { Text(label) }
}
