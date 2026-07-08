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
    Column {
        Text(
            text = "深色模式",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val darkModeOptions = remember {
            listOf(0 to "跟随系统", 1 to "浅色模式", 2 to "深色模式")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            darkModeOptions.forEach { (mode, label) ->
                val isSelected = currentDarkMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { onDarkModeChange(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = "主题色彩",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        val themeOptions = remember {
            val isMonetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            buildList {
                if (isMonetSupported) add(Pair(8, "莫奈取色" to Color.Unspecified))
                add(Pair(6, "红色" to RedPrimaryLight))
                add(Pair(2, "橙色" to OrangePrimaryLight))
                add(Pair(7, "黄色" to YellowPrimaryLight))
                add(Pair(1, "绿色" to GreenPrimaryLight))
                add(Pair(5, "青色" to TealPrimaryLight))
                add(Pair(0, "蓝色" to BluePrimaryLight))
                add(Pair(4, "紫色" to PurplePrimaryLight))
                add(Pair(3, "粉色" to PinkPrimaryLight))
            }
        }
        themeOptions.forEach { (index, pair) ->
            val (label, previewColor) = pair
            val isSelected = currentThemeColorIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onThemeColorIndexChange(index) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (index == 8) MaterialTheme.colorScheme.surfaceVariant else previewColor
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == 8) {
                        Icon(
                            painter = painterResource(android.R.drawable.star_on),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
