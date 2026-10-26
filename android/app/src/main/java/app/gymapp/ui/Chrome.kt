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
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
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
import app.gymapp.ui.glass.GlassLook
import app.gymapp.ui.glass.LiquidBottomTab
import app.gymapp.ui.glass.LiquidBottomTabs
import com.kyant.backdrop.Backdrop

internal val tabs = listOf(
    "Today" to Icons.Rounded.WbSunny,
    "Train" to Icons.Rounded.FitnessCenter,
    "You" to Icons.Rounded.Person,
)

/** Leaves room under the floating glass dock. */
internal val ScreenPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp)

/** Soft lotus light behind the content so the glass dock has color to refract (same idea as MindCore's Glow). */
@Composable
internal fun Glow() {
    val scheme = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        fun glow(color: Color, center: Offset, radius: Float) =
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
        glow(scheme.primary.copy(alpha = 0.22f), Offset(size.width * 0.1f, size.height * 0.06f), size.width * 0.9f)
        glow(scheme.tertiary.copy(alpha = 0.16f), Offset(size.width * 1.0f, size.height * 0.5f), size.width * 0.8f)
        glow(scheme.secondary.copy(alpha = 0.16f), Offset(0f, size.height * 0.95f), size.width * 0.8f)
    }
}

@Composable
internal fun BottomBar(
    selected: () -> Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    look: GlassLook,
    isLight: Boolean,
    modifier: Modifier,
) {
    val content = MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidBottomTabs(
            selectedTabIndex = selected,
            onTabSelected = onSelect,
            backdrop = backdrop,
            tabsCount = tabs.size,
            accentColor = MaterialTheme.colorScheme.primary,
            isLight = isLight,
            look = look,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                LiquidBottomTab({ onSelect(index) }) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = content)
                }
            }
        }
    }
}

@Composable
internal fun ScreenHeader(eyebrow: String, title: String) {
    Column(Modifier.statusBarsPadding().padding(top = 20.dp, bottom = 12.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
