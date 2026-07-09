package com.laofang.songshushoupai.songshu.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.ui.theme.RedPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.YellowPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.TealPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.BluePrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.GreenPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.OrangePrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.PinkPrimaryLight
import com.laofang.songshushoupai.songshu.ui.theme.PurplePrimaryLight

@Composable
fun ThemeSettingsCard(
    currentDarkMode: Int,
    onDarkModeChange: (Int) -> Unit,
    currentThemeColorIndex: Int,
    onThemeColorIndexChange: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("深色模式", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        val darkOpts = remember { listOf(0 to "跟随系统", 1 to "浅色模式", 2 to "深色模式") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            darkOpts.forEach { (mode, label) ->
                val sel = currentDarkMode == mode
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) cs.primary else cs.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onDarkModeChange(mode) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        color = if (sel) cs.onPrimary else cs.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("主题色彩", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)

        val themeOpts = remember {
            val monet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            buildList {
                if (monet) add(8 to ("莫奈取色" to Color.Unspecified))
                add(6 to ("红色" to RedPrimaryLight)); add(2 to ("橙色" to OrangePrimaryLight))
                add(7 to ("黄色" to YellowPrimaryLight)); add(1 to ("绿色" to GreenPrimaryLight))
                add(5 to ("青色" to TealPrimaryLight)); add(0 to ("蓝色" to BluePrimaryLight))
                add(4 to ("紫色" to PurplePrimaryLight)); add(3 to ("粉色" to PinkPrimaryLight))
            }
        }
        themeOpts.forEach { (index, pair) ->
            val (label, previewColor) = pair
            val sel = currentThemeColorIndex == index
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { onThemeColorIndexChange(index) }
                .background(if (sel) cs.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (index == 8) cs.surfaceVariant else previewColor),
                    contentAlignment = Alignment.Center) {
                    if (index == 8) Icon(painterResource(android.R.drawable.star_on), null,
                        Modifier.size(16.dp), cs.onSurfaceVariant)
                }
                Spacer(Modifier.width(14.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
                if (sel) Text("✓", style = MaterialTheme.typography.bodyMedium, color = cs.primary)
            }
        }
    }
}
