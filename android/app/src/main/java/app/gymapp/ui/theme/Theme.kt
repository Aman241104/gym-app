package app.gymapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Copied from MindCore (~/stash/app ui/theme/Theme.kt) so both apps share one look.
// Lotus palette, sampled from the app icon: pink petals, warm peach core, lavender glints.
val LotusDark = darkColorScheme(
    primary = Color(0xFFF5A9BE), onPrimary = Color(0xFF4A1426),
    primaryContainer = Color(0xFF6B2A3D), onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFF2BFA0), onSecondary = Color(0xFF45240F),
    tertiary = Color(0xFFBDB6F7), onTertiary = Color(0xFF26215C),
    background = Color(0xFF0B090A), onBackground = Color(0xFFEDE3E5),
    surface = Color(0xFF0B090A), onSurface = Color(0xFFEDE3E5),
    surfaceVariant = Color(0xFF3A3033), onSurfaceVariant = Color(0xFFCDBFC3),
    surfaceContainerLow = Color(0xFF161214), surfaceContainer = Color(0xFF1B1719),
    surfaceContainerHigh = Color(0xFF241F21), surfaceContainerHighest = Color(0xFF2F292B),
    outline = Color(0xFF978A8E), outlineVariant = Color(0xFF4D4346),
)

// Warm paper, ink text, one lotus accent; neutrals tinted a touch toward the pink.
val LotusLight = lightColorScheme(
    primary = Color(0xFFA23A5A), onPrimary = Color(0xFFFFFBFA),
    primaryContainer = Color(0xFFFFD9E1), onPrimaryContainer = Color(0xFF3E0619),
    secondary = Color(0xFF8C4F2A), onSecondary = Color(0xFFFFFBFA),
    secondaryContainer = Color(0xFFFFDCC7), onSecondaryContainer = Color(0xFF331200),
    tertiary = Color(0xFF5B55A8), onTertiary = Color(0xFFFFFBFA),
    tertiaryContainer = Color(0xFFE3DFFF), onTertiaryContainer = Color(0xFF161060),
    background = Color(0xFFFBF7F4), onBackground = Color(0xFF1F1A1C),
    surface = Color(0xFFFBF7F4), onSurface = Color(0xFF1F1A1C),
    surfaceVariant = Color(0xFFF1E6E7), onSurfaceVariant = Color(0xFF524346),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F1EE),
    surfaceContainer = Color(0xFFF3ECE9), surfaceContainerHigh = Color(0xFFEDE5E2),
    surfaceContainerHighest = Color(0xFFE6DEDB),
    outline = Color(0xFF857376), outlineVariant = Color(0xFFD8C2C5),
    inverseSurface = Color(0xFF1F1A1C), inverseOnSurface = Color(0xFFF7EEEF), inversePrimary = Color(0xFFF5A9BE),
    error = Color(0xFFBA1A1A),
)
