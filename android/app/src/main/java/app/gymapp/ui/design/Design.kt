package app.gymapp.ui.design

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.gymapp.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Instrument Serif Italic (SIL OFL): used once per heading for the warm, editorial phrase. */
val Serif = FontFamily(Font(R.font.instrument_serif_italic, style = FontStyle.Italic))

@Composable
fun SerifStyle(base: TextStyle = MaterialTheme.typography.displaySmall) = base.copy(fontFamily = Serif, fontStyle = FontStyle.Italic, letterSpacing = base.letterSpacing)

/** One accent per module, from MindCore's pastel set, so each area is recognisable at a glance. */
enum class Accent(private val light: Color, private val dark: Color) {
    Train(Color(0xFFA23A5A), Color(0xFFF5A9BE)), // lotus pink
    Run(Color(0xFFB0561E), Color(0xFFF2BFA0)), // peach
    Eat(Color(0xFF3F6B2E), Color(0xFFBFDCA8)), // sage
    Posture(Color(0xFF5B55A8), Color(0xFFBDB6F7)), // lavender
    Mind(Color(0xFF1F6C8A), Color(0xFFA9D8EC)); // sky

    val color: Color @Composable get() = if (isSystemInDarkTheme()) dark else light
}

/** Respect the system "remove animations" setting. */
@Composable
fun motionOn(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

/** Fades and lifts content in on first appearance; [order] staggers siblings by 45 ms. */
fun Modifier.rise(order: Int = 0): Modifier = composed {
    val on = motionOn()
    val progress = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (on) progress.animateTo(1f, tween(durationMillis = 520, delayMillis = order.coerceAtMost(8) * 45, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 18.dp.toPx()
    }
}

/** Click with a soft spring squeeze and a light haptic tick. */
fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press")
    val haptics = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, indication = null) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** A number that counts up to [value] when it first shows or changes. */
@Composable
fun CountUp(value: Int, style: TextStyle, color: Color = Color.Unspecified, suffix: String = "") {
    val shown by animateIntAsState(value, tween(if (motionOn()) 900 else 0, easing = FastOutSlowInEasing), label = "count")
    Text("$shown$suffix", style = style, color = color)
}

data class RingSpec(val progress: Float, val color: Color)

/** Apple-Fitness-style concentric rings, outermost first. Each draws in with a spring. */
@Composable
fun Rings(rings: List<RingSpec>, modifier: Modifier, stroke: Dp = 14.dp, gap: Dp = 5.dp) {
    val on = motionOn()
    val animated = rings.mapIndexed { i, r ->
        val start = remember { Animatable(if (on) 0f else r.progress) }
        LaunchedEffect(r.progress) { start.animateTo(r.progress, spring(dampingRatio = 0.8f, stiffness = 60f - i * 6f)) }
        start.value
    }
    Canvas(modifier) {
        val w = stroke.toPx()
        rings.forEachIndexed { i, r ->
            val inset = w / 2 + i * (w + gap.toPx())
            val size = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(r.color.copy(alpha = 0.10f), 0f, 360f, false, topLeft, size, style = Stroke(w))
            val sweep = 360f * animated[i].coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    Brush.sweepGradient(listOf(r.color.copy(alpha = 0.75f), r.color, r.color.copy(alpha = 0.75f)), center = Offset(this.size.width / 2, this.size.height / 2)),
                    -90f, sweep, false, topLeft, size, style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/**
 * Slow-drifting lotus light behind everything (replaces the static glow), so the glass dock always
 * has colour to bend. One infinite transition, three blurred blobs: cheap enough for every frame.
 */
@Composable
fun Aurora() {
    val scheme = MaterialTheme.colorScheme
    val on = motionOn()
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, 1f, infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val phase = if (on) t * 2 * PI.toFloat() else 0f
    val peach = Accent.Run.color
    val lavender = Accent.Posture.color
    Canvas(Modifier.fillMaxSize()) {
        fun blob(color: Color, cx: Float, cy: Float, r: Float) {
            val c = Offset(cx * size.width, cy * size.height)
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), c, r * size.width), r * size.width, c)
        }
        blob(scheme.primary.copy(alpha = 0.20f), 0.15f + 0.08f * cos(phase), 0.05f + 0.04f * sin(phase), 0.95f)
        blob(lavender.copy(alpha = 0.14f), 0.95f + 0.06f * sin(phase), 0.45f + 0.06f * cos(phase), 0.8f)
        blob(peach.copy(alpha = 0.12f), 0.05f + 0.07f * cos(phase + 1f), 0.95f + 0.03f * sin(phase), 0.85f)
    }
}

/** Fades content out under the status bar so the clock never sits on top of a card. */
@Composable
fun StatusScrim(modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 16.dp)))
            .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.8f), Color.Transparent))),
    )
}
