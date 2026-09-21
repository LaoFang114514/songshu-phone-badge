package com.laofang.songshushoupai.songshu.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.R

private val TutorialSectionShape = RoundedCornerShape(16.dp)

@Composable
fun TutorialSettingsCard() {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 欢迎横幅
        Card(
            modifier = Modifier.fillMaxWidth().border(tutorialCardBorder(), TutorialSectionShape).clip(TutorialSectionShape),
            shape = TutorialSectionShape,
            colors = CardDefaults.cardColors(containerColor = cs.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TutorialIconBadge(Icons.Filled.School)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tutorial_welcome_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tutorial_welcome_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        TutorialSection(
            title = stringResource(R.string.tutorial_section_basic),
            icon = Icons.Filled.PhotoLibrary,
            initiallyExpanded = true,
            steps = listOf(
                stringResource(R.string.tut_add_image_title) to stringResource(R.string.tut_add_image_desc),
                stringResource(R.string.tut_add_video_title) to stringResource(R.string.tut_add_video_desc),
                stringResource(R.string.tut_manage_title) to stringResource(R.string.tut_manage_desc),
                stringResource(R.string.tut_select_title) to stringResource(R.string.tut_select_desc),
            )
        )

        TutorialSection(
            title = stringResource(R.string.tutorial_section_display),
            icon = Icons.Filled.Smartphone,
            steps = listOf(
                stringResource(R.string.tut_start_title) to stringResource(R.string.tut_start_desc),
                stringResource(R.string.tut_gesture_rotate_title) to stringResource(R.string.tut_gesture_rotate_desc),
                stringResource(R.string.tut_gesture_exit_title) to stringResource(R.string.tut_gesture_exit_desc),
                stringResource(R.string.tut_gesture_qr_title) to stringResource(R.string.tut_gesture_qr_desc),
                stringResource(R.string.tut_qr_swipe_title) to stringResource(R.string.tut_qr_swipe_desc),
                stringResource(R.string.tut_battery_title) to stringResource(R.string.tut_battery_desc),
            )
        )

        TutorialSection(
            title = stringResource(R.string.tutorial_section_settings),
            icon = Icons.Filled.Settings,
            steps = listOf(
                stringResource(R.string.tut_reverse_title) to stringResource(R.string.tut_reverse_desc),
                stringResource(R.string.tut_screen_on_title) to stringResource(R.string.tut_screen_on_desc),
                stringResource(R.string.tut_anti_burnin_title) to stringResource(R.string.tut_anti_burnin_desc),
                stringResource(R.string.tut_qr_code_title) to stringResource(R.string.tut_qr_code_desc),
                stringResource(R.string.tut_theme_title) to stringResource(R.string.tut_theme_desc),
                stringResource(R.string.tut_language_title) to stringResource(R.string.tut_language_desc),
            )
        )

        TutorialSection(
            title = stringResource(R.string.tutorial_section_advanced),
            icon = Icons.Filled.Backup,
            steps = listOf(
                stringResource(R.string.tut_backup_title) to stringResource(R.string.tut_backup_desc),
            )
        )
    }
}

@Composable
private fun tutorialCardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun TutorialIconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.10f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(9.dp).size(20.dp),
            tint = tint
        )
    }
}

@Composable
private fun TutorialSection(
    title: String,
    icon: ImageVector,
    steps: List<Pair<String, String>>,
    initiallyExpanded: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "tutorialSectionArrow"
    )

    Card(
        modifier = Modifier.fillMaxWidth().border(tutorialCardBorder(), TutorialSectionShape).clip(TutorialSectionShape),
        shape = TutorialSectionShape,
        colors = CardDefaults.cardColors(containerColor = cs.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TutorialIconBadge(icon)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // 步骤数量角标
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = cs.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = steps.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).rotate(arrowRotation),
                    tint = cs.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    steps.forEachIndexed { index, (stepTitle, stepDesc) ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = cs.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                        TutorialStep(number = index + 1, title = stepTitle, description = stepDesc)
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialStep(number: Int, title: String, description: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 序号圆形角标
        Box(
            modifier = Modifier.size(24.dp).background(color = cs.primary.copy(alpha = 0.10f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = cs.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}
