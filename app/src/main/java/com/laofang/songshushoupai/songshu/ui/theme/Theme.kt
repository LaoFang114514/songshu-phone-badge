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

private val DefaultLight = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
private val DefaultDark = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private fun scheme(l: Color, ld: Color, s: Color, sd: Color, t: Color, td: Color) =
    Pair(lightColorScheme(primary = l, secondary = s, tertiary = t), darkColorScheme(primary = ld, secondary = sd, tertiary = td))

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