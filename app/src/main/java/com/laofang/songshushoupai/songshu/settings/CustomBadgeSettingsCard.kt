package com.laofang.songshushoupai.songshu.settings

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.laofang.songshushoupai.songshu.R
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val QUQIMENG_LOGO_URL = "https://quqimeng.com/Web/res/logo.png"
private const val QUQIMENG_MARKET_URL = "https://uc.qimeng.fun/module_page/page_jiegao/"

/**
 * 「兽牌定制」独立页面内容。
 *
 * 目前该功能仍在筹备中，这里以「敬请期待」空状态呈现：
 * 一个带呼吸光晕的图标、状态标签、标题与说明文案，替代原先单薄的占位卡片。
 */
@Composable
fun CustomBadgeSettingsCard() {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    // 「趣绮梦稿件集市」logo 远程加载，加载完成前展示占位图标
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                URL(QUQIMENG_LOGO_URL).openStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }
        if (bmp != null) logoBitmap = bmp.asImageBitmap()
    }

    // 图标外圈光晕的呼吸动效
    val pulse = remember { Animatable(0.88f) }
    LaunchedEffect(Unit) {
        pulse.animateTo(
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 「趣绮梦稿件集市」合作服务卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, cs.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = cs.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, QUQIMENG_MARKET_URL.toUri()))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, ctx.getString(R.string.jump_failed, e.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val logo = logoBitmap
                    if (logo != null) {
                        Image(
                            bitmap = logo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            Icons.Filled.Store,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = cs.onSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.quqimeng_badge_market),
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.quqimeng_service_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant
                )
            }
        }

        Text(
            stringResource(R.string.custom_badge_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
