package uzair.lightpods.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PodPrimaryDark,
    onPrimary = PodWhite,
    secondary = PodOnSurfaceVariant,
    onSecondary = PodWhite,
    tertiary = BatteryFull,
    background = PodBackground,
    onBackground = PodOnBackground,
    surface = PodSurface,
    onSurface = PodOnSurface,
    surfaceVariant = PodSurfaceVariant,
    onSurfaceVariant = PodOnSurfaceVariant,
    outline = PodOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PodPrimaryLight,
    onPrimary = PodWhite,
    secondary = PodOnSurfaceVariantLight,
    onSecondary = PodWhite,
    tertiary = BatteryFull,
    background = PodBackgroundLight,
    onBackground = PodOnSurfaceLight,
    surface = PodWhite,
    onSurface = PodOnSurfaceLight,
    surfaceVariant = PodOffWhite,
    onSurfaceVariant = PodOnSurfaceVariantLight,
    outline = PodOnSurfaceVariantLight
)

@Composable
fun LightPodsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}