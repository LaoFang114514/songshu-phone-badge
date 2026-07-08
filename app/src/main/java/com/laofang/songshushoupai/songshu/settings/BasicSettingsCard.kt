package com.laofang.songshushoupai.songshu.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable

@Composable
fun BasicSettingsCard(
    defaultOrientation: Boolean,
    onDefaultOrientationChange: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    showBattery: Boolean,
    onShowBatteryChange: (Boolean) -> Unit,
    lockOrientation: Boolean,
    onLockOrientationChange: (Boolean) -> Unit,
    antiBurnIn: Boolean,
    onAntiBurnInChange: (Boolean) -> Unit,
    muteVideo: Boolean,
    onMuteVideoChange: (Boolean) -> Unit
) {
    Column {
        SettingsSwitchRow("反向显示", "打开此开关即为反向显示", defaultOrientation) {
            onDefaultOrientationChange(it)
        }
        HorizontalDivider()
        SettingsSwitchRow("常亮显示", "启动时屏幕保持常亮", keepScreenOn) {
            onKeepScreenOnChange(it)
        }
        HorizontalDivider()
        SettingsSwitchRow("显示电池电量", "在全屏播放时顶部显示电池电量", showBattery) {
            onShowBatteryChange(it)
        }
        HorizontalDivider()
        SettingsSwitchRow("锁定方向", "锁定后长按不会旋转兽牌", lockOrientation) {
            onLockOrientationChange(it)
        }
        HorizontalDivider()
        SettingsSwitchRow("防烧屏", "每隔5分钟轻微移动，防止屏幕残影", antiBurnIn) {
            onAntiBurnInChange(it)
        }
        HorizontalDivider()
        SettingsSwitchRow("视频静音", "播放视频时默认静音", muteVideo) {
            onMuteVideoChange(it)
        }
    }
}
