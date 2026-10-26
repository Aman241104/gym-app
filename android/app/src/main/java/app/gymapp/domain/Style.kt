package app.gymapp.domain

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

val GarmentKinds = listOf("Top", "Bottom", "Shoes", "Layer", "Accessory")

/** A wardrobe piece. [photo] is the cut-out PNG in app storage (phone-only); [rgb] its main colour. */
@Serializable
data class Garment(val kind: String, val colour: String, val rgb: Int, val photo: String)

/** Saved outfit: garment item ids by kind. */
@Serializable
data class Outfit(val pieces: Map<String, String>, val savedOn: String)

private val named = listOf(
    "Black" to 0x111111, "Charcoal" to 0x3C3C3C, "Grey" to 0x8A8A8A, "White" to 0xF4F4F2, "Cream" to 0xEFE6D2,
    "Beige" to 0xD6C3A0, "Brown" to 0x6B4A2E, "Navy" to 0x1F2A4A, "Blue" to 0x2F6FD0, "Light blue" to 0x9CC3E8,
    "Denim" to 0x3E5C80, "Green" to 0x2E7D4F, "Olive" to 0x6B6B2E, "Red" to 0xC0312B, "Maroon" to 0x6E1E2A,
    "Pink" to 0xE89AB2, "Orange" to 0xE07A2E, "Yellow" to 0xE8C547, "Purple" to 0x6E4A9E,
)

/** Nearest named colour by simple RGB distance; good enough to label clothes. */
fun colourName(rgb: Int): String = named.minBy { (_, c) ->
    val dr = (rgb shr 16 and 255) - (c shr 16 and 255)
    val dg = (rgb shr 8 and 255) - (c shr 8 and 255)
    val db = (rgb and 255) - (c and 255)
    2 * dr * dr + 4 * dg * dg + 3 * db * db
}.first

private val neutrals = setOf("Black", "Charcoal", "Grey", "White", "Cream", "Beige", "Brown", "Navy", "Denim")

private fun saturation(rgb: Int): Float {
    val r = (rgb shr 16 and 255) / 255f; val g = (rgb shr 8 and 255) / 255f; val b = (rgb and 255) / 255f
    val hi = max(r, max(g, b)); val lo = min(r, min(g, b))
    return if (hi == 0f) 0f else (hi - lo) / hi
}

/** One line of advice for an outfit: neutrals go with anything; two bold colours usually fight. */
fun harmonyHint(pieces: List<Garment>): String {
    val bold = pieces.filter { it.colour !in neutrals && saturation(it.rgb) > 0.35f }.map { it.colour }.distinct()
    return when {
        pieces.size < 2 -> "Pick at least a top and a bottom."
        bold.isEmpty() -> "All neutrals: clean and easy. Add one colour accent if it feels flat."
        bold.size == 1 -> "One colour against neutrals: balanced."
        else -> "${bold.joinToString(" + ")}: two strong colours compete. Swap one for a neutral."
    }
}
