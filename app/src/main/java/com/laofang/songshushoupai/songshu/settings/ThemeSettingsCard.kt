package com.laofang.songshushoupai.songshu.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R
import com.laofang.songshushoupai.songshu.ui.theme.*

@Composable
fun ThemeSettingsCard(currentDarkMode: Int, onDarkModeChange: (Int) -> Unit, currentThemeColorIndex: Int, onThemeColorIndexChange: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        val darkOpts = remember { listOf(0 to R.string.follow_system, 1 to R.string.light_mode, 2 to R.string.dark_mode_label) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            darkOpts.forEach { (mode, labelRes) ->
                val sel = currentDarkMode == mode
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) cs.primary else cs.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onDarkModeChange(mode) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall,
                        color = if (sel) cs.onPrimary else cs.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.theme_color), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        val themeOpts = remember {
            val monet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            buildList {
                if (monet) add(8 to (R.string.monet_color to Color.Unspecified))
                add(6 to (R.string.color_red to RedPrimaryLight)); add(2 to (R.string.color_orange to OrangePrimaryLight))
                add(7 to (R.string.color_yellow to YellowPrimaryLight)); add(1 to (R.string.color_green to GreenPrimaryLight))
                add(5 to (R.string.color_teal to TealPrimaryLight)); add(0 to (R.string.color_blue to BluePrimaryLight))
                add(4 to (R.string.color_purple to PurplePrimaryLight)); add(3 to (R.string.color_pink to PinkPrimaryLight))
            }
        }
        themeOpts.forEach { (index, pair) ->
            val (label, previewColor) = pair
            val sel = currentThemeColorIndex == index
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { onThemeColorIndexChange(index) }
                .background(if (sel) cs.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (index == 8) cs.surfaceVariant else previewColor),
                    contentAlignment = Alignment.Center) {
                    if (index == 8) Icon(painterResource(android.R.drawable.star_on), null, Modifier.size(16.dp), cs.onSurfaceVariant)
                }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
                if (sel) Text("✓", style = MaterialTheme.typography.bodyMedium, color = cs.primary)
            }
        }
    }
}
