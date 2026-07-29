package com.laofang.songshushoupai.songshu.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R

@Composable
fun TutorialSettingsCard() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.tutorial_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tutorial_welcome_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TutorialSection(stringResource(R.string.tutorial_section_basic)) {
            TutorialStep(stringResource(R.string.tut_add_image_title), stringResource(R.string.tut_add_image_desc))
            TutorialStep(stringResource(R.string.tut_add_video_title), stringResource(R.string.tut_add_video_desc))
            TutorialStep(stringResource(R.string.tut_manage_title), stringResource(R.string.tut_manage_desc))
            TutorialStep(stringResource(R.string.tut_select_title), stringResource(R.string.tut_select_desc))
        }

        TutorialSection(stringResource(R.string.tutorial_section_display)) {
            TutorialStep(stringResource(R.string.tut_start_title), stringResource(R.string.tut_start_desc))
            TutorialStep(stringResource(R.string.tut_gesture_rotate_title), stringResource(R.string.tut_gesture_rotate_desc))
            TutorialStep(stringResource(R.string.tut_gesture_exit_title), stringResource(R.string.tut_gesture_exit_desc))
            TutorialStep(stringResource(R.string.tut_gesture_qr_title), stringResource(R.string.tut_gesture_qr_desc))
            TutorialStep(stringResource(R.string.tut_battery_title), stringResource(R.string.tut_battery_desc))
        }

        TutorialSection(stringResource(R.string.tutorial_section_settings)) {
            TutorialStep(stringResource(R.string.tut_reverse_title), stringResource(R.string.tut_reverse_desc))
            TutorialStep(stringResource(R.string.tut_screen_on_title), stringResource(R.string.tut_screen_on_desc))
            TutorialStep(stringResource(R.string.tut_anti_burnin_title), stringResource(R.string.tut_anti_burnin_desc))
            TutorialStep(stringResource(R.string.tut_qr_code_title), stringResource(R.string.tut_qr_code_desc))
            TutorialStep(stringResource(R.string.tut_theme_title), stringResource(R.string.tut_theme_desc))
            TutorialStep(stringResource(R.string.tut_language_title), stringResource(R.string.tut_language_desc))
        }

        TutorialSection(stringResource(R.string.tutorial_section_advanced)) {
            TutorialStep(stringResource(R.string.tut_backup_title), stringResource(R.string.tut_backup_desc))
        }
    }
}

@Composable
private fun TutorialSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun TutorialStep(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
