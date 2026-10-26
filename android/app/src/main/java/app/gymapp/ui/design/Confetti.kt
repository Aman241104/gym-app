package app.gymapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Piece(val x: Float, val delay: Float, val speed: Float, val drift: Float, val spin: Float, val color: Color, val w: Float)

/** A short burst of falling confetti in the module colours. Only used for personal records. */
@Composable
fun Confetti(colors: List<Color>, modifier: Modifier = Modifier.fillMaxSize()) {
    val pieces = remember {
        val r = Random(System.nanoTime())
        List(90) { Piece(r.nextFloat(), r.nextFloat() * 0.35f, 0.7f + r.nextFloat() * 0.6f, r.nextFloat() * 2 - 1, r.nextFloat() * 720 - 360, colors[it % colors.size], 6f + r.nextFloat() * 8) }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(modifier) {
        pieces.forEach { p ->
            val local = ((t.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) return@forEach
            val y = -20f + local * p.speed * size.height * 1.1f
            val x = p.x * size.width + p.drift * 80f * local
            rotate(p.spin * local, Offset(x, y)) {
                drawRect(p.color.copy(alpha = 1f - local * 0.6f), Offset(x, y), Size(p.w, p.w * 1.6f))
            }
        }
    }
}
