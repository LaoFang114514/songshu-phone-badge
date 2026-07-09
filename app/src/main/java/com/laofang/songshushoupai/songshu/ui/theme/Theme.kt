package com.laofang.songshushoupai.songshu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun Color.luminance(): Float {
    fun linearize(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).let { it * it * it }
    return 0.2126f * linearize(red) + 0.7152f * linearize(green) + 0.0722f * linearize(blue)
}

private fun Color.onColor(): Color = if (luminance() > 0.3f) Color.Black else Color.White

private val DefaultLight = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
private val DefaultDark = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private fun scheme(l: Color, ld: Color, s: Color, sd: Color, t: Color, td: Color) =
    Pair(
        lightColorScheme(
            primary = l, secondary = s, tertiary = t,
            onPrimary = l.onColor(), onSecondary = s.onColor(), onTertiary = t.onColor(),
            primaryContainer = l.copy(alpha = 0.15f),
            onPrimaryContainer = l.onColor()
        ),
        darkColorScheme(
            primary = ld, secondary = sd, tertiary = td,
            onPrimary = ld.onColor(), onSecondary = sd.onColor(), onTertiary = td.onColor(),
            primaryContainer = ld.copy(alpha = 0.15f),
            onPrimaryContainer = if (ld.luminance() > 0.3f) ld.copy(red = 0.15f, green = 0.15f, blue = 0.15f) else ld
        )
    )

private val schemes = listOf(
    scheme(BluePrimaryLight, BluePrimaryDark, BlueSecondaryLight, BlueSecondaryDark, BlueTertiaryLight, BlueTertiaryDark),
    scheme(GreenPrimaryLight, GreenPrimaryDark, GreenSecondaryLight, GreenSecondaryDark, GreenTertiaryLight, GreenTertiaryDark),
    scheme(OrangePrimaryLight, OrangePrimaryDark, OrangeSecondaryLight, OrangeSecondaryDark, OrangeTertiaryLight, OrangeTertiaryDark),
    scheme(PinkPrimaryLight, PinkPrimaryDark, PinkSecondaryLight, PinkSecondaryDark, PinkTertiaryLight, PinkTertiaryDark),
    scheme(PurplePrimaryLight, PurplePrimaryDark, PurpleSecondaryLight, PurpleSecondaryDark, PurpleTertiaryLight, PurpleTertiaryDark),
    scheme(TealPrimaryLight, TealPrimaryDark, TealSecondaryLight, TealSecondaryDark, TealTertiaryLight, TealTertiaryDark),
    scheme(RedPrimaryLight, RedPrimaryDark, RedSecondaryLight, RedSecondaryDark, RedTertiaryLight, RedTertiaryDark),
    scheme(YellowPrimaryLight, YellowPrimaryDark, YellowSecondaryLight, YellowSecondaryDark, YellowTertiaryLight, YellowTertiaryDark),
    scheme(BluePrimaryLight, BluePrimaryDark, BlueSecondaryLight, BlueSecondaryDark, BlueTertiaryLight, BlueTertiaryDark),
)

fun getPresetColorScheme(index: Int, dark: Boolean): androidx.compose.material3.ColorScheme {
    if (index !in schemes.indices) return if (dark) DefaultDark else DefaultLight
    val (l, d) = schemes[index]
    return if (dark) d else l
}

@Composable
fun SongshushoupaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeColorIndex == 8 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else {
        getPresetColorScheme(themeColorIndex, darkTheme)
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}