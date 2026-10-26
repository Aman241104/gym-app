package app.gymapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.gymapp.ui.design.Serif
import app.gymapp.ui.design.pressable
import app.gymapp.ui.design.rise
import androidx.compose.foundation.border
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import app.gymapp.ui.glass.GlassLook
import app.gymapp.ui.glass.LiquidButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.vector.ImageVector
import app.gymapp.ui.glass.LiquidBottomTab
import app.gymapp.ui.glass.LiquidBottomTabs
import com.kyant.backdrop.Backdrop

internal val tabs = listOf(
    "Today" to Icons.Rounded.WbSunny,
    "Train" to Icons.Rounded.FitnessCenter,
    "Run" to Icons.AutoMirrored.Rounded.DirectionsRun,
    "Eat" to Icons.Rounded.Restaurant,
    "You" to Icons.Rounded.Person,
)

/** Leaves room under the floating glass dock. */
internal val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp)

/** The dock's context button: what the current page's main action is. */
internal data class DockAction(val icon: ImageVector, val label: String, val tint: Color, val onClick: () -> Unit)

/**
 * Glass tab bar plus a context button on the right that morphs per page (start, run, scan, sync),
 * the iOS 26 "tab bar + accessory" pattern built on the same Kyant backdrop.
 */
@Composable
internal fun BottomBar(
    selected: () -> Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    look: GlassLook,
    isLight: Boolean,
    action: DockAction?,
    modifier: Modifier,
) {
    val content = MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LiquidBottomTabs(
            selectedTabIndex = selected,
            onTabSelected = onSelect,
            backdrop = backdrop,
            tabsCount = tabs.size,
            accentColor = MaterialTheme.colorScheme.primary,
            isLight = isLight,
            look = look,
            modifier = Modifier.weight(1f),
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                LiquidBottomTab({ onSelect(index) }) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
                }
            }
        }
        if (action != null) {
            LiquidButton(onClick = action.onClick, backdrop = backdrop, tint = action.tint, modifier = Modifier.size(64.dp)) {
                AnimatedContent(action, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, contentKey = { it.label }, label = "dock") { a ->
                    Icon(a.icon, contentDescription = a.label, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

/**
 * Screen title in the Oura-style duet: a bold grotesque [title] and, after it, one warm
 * [serif] italic phrase in the screen's accent colour.
 */
@Composable
internal fun ScreenHeader(eyebrow: String, title: String, serif: String? = null, accent: Color = MaterialTheme.colorScheme.primary) {
    Column(Modifier.statusBarsPadding().padding(top = 24.dp, bottom = 10.dp).rise()) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp), color = accent)
        Text(
            buildAnnotatedString {
                append(title)
                if (serif != null) {
                    append(" ")
                    withStyle(SpanStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, color = accent)) { append(serif) }
                }
            },
            style = MaterialTheme.typography.displaySmall,
        )
    }
}

/**
 * The card every screen is built from: soft vertical gradient, a hairline edge, an optional
 * accent wash in the corner, rises in on first show, and squeezes + ticks when tappable.
 */
@Composable
internal fun Panel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .rise()
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHigh.copy(alpha = 0.82f), scheme.surfaceContainer.copy(alpha = 0.66f))))
            .then(if (accent != null) Modifier.background(Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), center = Offset.Zero, radius = 700f)) else Modifier)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
