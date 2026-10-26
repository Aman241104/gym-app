package app.gymapp.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Product types in the order they go on the face. */
val SkinTypes = listOf(
    "Cleanser", "Toner", "Vitamin C", "Niacinamide", "Hyaluronic acid", "AHA/BHA exfoliant",
    "Retinoid", "Benzoyl peroxide", "Eye cream", "Moisturiser", "Sunscreen", "Other",
)

/** [whenUsed] is "AM", "PM" or "Both"; [paoMonths] is the jar's "period after opening". */
@Serializable
data class SkinProduct(
    val name: String,
    val type: String,
    val whenUsed: String = "Both",
    val openedOn: String? = null,
    val paoMonths: Int? = null,
) {
    fun usedIn(slot: String) = whenUsed == "Both" || whenUsed == slot
    fun expired(today: LocalDate): Boolean =
        openedOn != null && paoMonths != null && LocalDate.parse(openedOn).plusMonths(paoMonths.toLong()).isBefore(today)
}

/** Ticked steps for one day, stored as its own item ("log-yyyy-mm-dd"). */
@Serializable
data class SkinDay(val am: List<String> = emptyList(), val pm: List<String> = emptyList())

/**
 * Well-known combinations to keep apart in the same routine. A short hand-written table shown as
 * reminders, not medical advice.
 */
private val clashes = listOf(
    Triple("Retinoid", "AHA/BHA exfoliant", "Retinoid and acids together can irritate. Use them on different nights."),
    Triple("Retinoid", "Benzoyl peroxide", "Benzoyl peroxide can deactivate some retinoids. Split them AM/PM or by day."),
    Triple("Retinoid", "Vitamin C", "Vitamin C suits mornings; keep retinoid for night."),
    Triple("AHA/BHA exfoliant", "Vitamin C", "Acids plus vitamin C can sting. Alternate, or use vitamin C in the morning."),
)

fun skinWarnings(products: List<SkinProduct>, slot: String): List<String> {
    val types = products.filter { it.usedIn(slot) }.map { it.type }.toSet()
    val out = clashes.filter { (a, b, _) -> a in types && b in types }.map { it.third }.toMutableList()
    val all = products.map { it.type }.toSet()
    if (slot == "AM" && ("Retinoid" in all || "AHA/BHA exfoliant" in all) && "Sunscreen" !in products.filter { it.usedIn("AM") }.map { it.type }) {
        out += "You use a retinoid or acid: add sunscreen to your morning routine."
    }
    return out
}

fun routine(products: List<SkinProduct>, slot: String): List<SkinProduct> =
    products.filter { it.usedIn(slot) && !(slot == "PM" && it.type == "Sunscreen") }.sortedBy { SkinTypes.indexOf(it.type) }
