package com.laofang.songshushoupai.songshu.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(vertical = 8.dp))

@Composable
fun BasicSettingsCard(
    defaultOrientation: Boolean, onDefaultOrientationChange: (Boolean) -> Unit,
    keepScreenOn: Boolean, onKeepScreenOnChange: (Boolean) -> Unit,
    showBattery: Boolean, onShowBatteryChange: (Boolean) -> Unit,
    lockOrientation: Boolean, onLockOrientationChange: (Boolean) -> Unit,
    antiBurnIn: Boolean, onAntiBurnInChange: (Boolean) -> Unit,
    muteVideo: Boolean, onMuteVideoChange: (Boolean) -> Unit,
    languageIndex: Int, onLanguageChange: (Int) -> Unit
) {
    val langOpts = remember { listOf(0 to R.string.lang_chinese, 1 to R.string.lang_english) }
    var expanded by remember { mutableStateOf(false) }
    val currentLabelRes = remember(languageIndex) {
        langOpts.find { it.first == languageIndex }?.second ?: R.string.lang_chinese
    }
    val currentLabel = stringResource(currentLabelRes)
    val cs = MaterialTheme.colorScheme

    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                Text(stringResource(R.string.language_desc), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, cs.outlineVariant), RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.primary
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = cs.primary
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(BorderStroke(1.dp, cs.outlineVariant), RoundedCornerShape(12.dp))
                ) {
                    langOpts.forEach { (index, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            onClick = {
                                expanded = false
                                onLanguageChange(index)
                            }
                        )
                    }
                }
            }
        }
        Divider()
        SettingsSwitchRow(stringResource(R.string.reverse_display), stringResource(R.string.reverse_display_desc), defaultOrientation, onDefaultOrientationChange)
        Divider()
        SettingsSwitchRow(stringResource(R.string.keep_screen_on), stringResource(R.string.keep_screen_on_desc), keepScreenOn, onKeepScreenOnChange)
        Divider()
        SettingsSwitchRow(stringResource(R.string.show_battery), stringResource(R.string.show_battery_desc), showBattery, onShowBatteryChange)
        Divider()
        SettingsSwitchRow(stringResource(R.string.lock_orientation), stringResource(R.string.lock_orientation_desc), lockOrientation, onLockOrientationChange)
        Divider()
        SettingsSwitchRow(stringResource(R.string.anti_burn_in), stringResource(R.string.anti_burn_in_desc), antiBurnIn, onAntiBurnInChange)
        Divider()
        SettingsSwitchRow(stringResource(R.string.mute_video), stringResource(R.string.mute_video_desc), muteVideo, onMuteVideoChange)
    }
}
