package com.laofang.songshushoupai.songshu.start.egg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laofang.songshushoupai.songshu.SettingsManager
import com.laofang.songshushoupai.songshu.ui.theme.SongshushoupaiTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ColorSudokuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val s = remember { SettingsManager.loadSettings(ctx) }
            val dark = when (s.darkMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            SongshushoupaiTheme(darkTheme = dark, themeColorIndex = s.themeColorIndex) {
                ColorSudokuScreen()
            }
        }
    }
}

private val targetPattern = listOf(
    listOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true, false, false, false, false, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true, false, false, false, false, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true, false, false, false, false, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true, false, false,  true, false, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false, false, false, false, false,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false, false, false, false, false,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false, false, false, false, false,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false, false, false, false, false,  true,  true,  true,  true, false, false, false),
    listOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSudokuScreen() {
    var userGrid by remember {
        mutableStateOf(List(14) { List(14) { false } })
    }
    var flashPhase by remember { mutableIntStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var gridSize by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val isComplete = (0 until 14).all { r ->
        (0 until 14).all { c -> userGrid[r][c] == targetPattern[r][c] }
    }

    LaunchedEffect(isComplete) {
        if (isComplete && !showHint) {
            for (i in 0 until 6) {
                flashPhase = (i % 2) + 1
                delay(200.milliseconds)
            }
            flashPhase = 0
            showHint = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .onGloballyPositioned { coordinates ->
                        gridSize = coordinates.size.width.toFloat()
                    }
            ) {
                val cellSizePx = gridSize / 14f
                val spacingPx = 2f * density.density
                val adjustedCellSizePx = cellSizePx - spacingPx

                for (row in 0 until 14) {
                    for (col in 0 until 14) {
                        val isFilled = userGrid[row][col]
                        val cellColor = when {
                            showHint -> when {
                                targetPattern[row][col] -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                            flashPhase == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            flashPhase == 2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (col * cellSizePx + spacingPx / 2f).toDp() },
                                    y = with(density) { (row * cellSizePx + spacingPx / 2f).toDp() }
                                )
                                .size(with(density) { adjustedCellSizePx.toDp() })
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellColor)
                                .border(
                                    1.dp,
                                    if (isFilled || showHint) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable {
                                    if (!showHint) {
                                        userGrid = userGrid.mapIndexed { r, rowList ->
                                            if (r == row) rowList.mapIndexed { c, v ->
                                                if (c == col) !v else v
                                            } else rowList
                                        }
                                    }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (showHint) "老方-LaoFang" else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = if (showHint) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (showHint) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

