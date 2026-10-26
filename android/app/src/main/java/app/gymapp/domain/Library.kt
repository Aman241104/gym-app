package app.gymapp.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One entry of free-exercise-db (github.com/yuhonas/free-exercise-db, Unlicense / public domain),
 * bundled as assets/exercises.json. Images are served from the repo through jsDelivr.
 */
@Serializable
data class LibExercise(
    val id: String,
    val name: String,
    val equipment: String? = null,
    val category: String,
    val level: String,
    val force: String? = null,
    val mechanic: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val images: List<String> = emptyList(),
) {
    val imageUrls: List<String> get() = images.map { "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises/$it" }
}

private val libJson = Json { ignoreUnknownKeys = true }

fun parseLibrary(text: String): List<LibExercise> = libJson.decodeFromString(ListSerializer(LibExercise.serializer()), text)

/**
 * Can this exercise be done with the ticked equipment? free-exercise-db only knows 12 coarse
 * equipment types, so named machines, benches, racks and bars are matched on the exercise name.
 */
fun LibExercise.doableWith(kit: Set<String>): Boolean {
    val n = name.lowercase()
    fun has(vararg items: String) = items.any { it in kit }
    fun named(vararg words: String) = words.any { it in n }

    // Things the name says it needs, whatever the coarse equipment type is.
    if (named("smith") && !has("Smith machine")) return false
    if (named("incline", "decline") && named("bench", "press", "fly", "flye", "row", "curl") && !has("Adjustable bench")) return false
    if (named("bench") && !named("dip") && !has("Flat bench", "Adjustable bench", "Smith machine")) return false
    if (named("pullup", "pull-up", "chin-up", "chinup", "chin up") && !named("assisted", "band", "lat pulldown") && !has("Pull-up bar")) return false
    if (named("dips") && !named("bench dip") && !has("Dip station", "Assisted pull-up machine")) return false

    return when (equipment) {
        "body only" -> true
        null, "other" -> category == "stretching"
        "dumbbell" -> has("Dumbbells", "Adjustable dumbbells")
        "barbell" -> has("Barbell") && (!named("squat", "rack") || named("front squat to", "overhead") || has("Squat / power rack", "Smith machine"))
        "e-z curl bar" -> has("EZ bar")
        "kettlebells" -> has("Kettlebells")
        "bands" -> has("Resistance bands", "Mini bands")
        "medicine ball" -> has("Medicine ball")
        "exercise ball" -> has("Stability ball")
        "foam roll" -> has("Foam roller")
        "cable" -> has("Cable machine") ||
            (has("Seated row machine") && named("seated cable row", "seated row")) ||
            (has("Lat pulldown") && named("pulldown", "pull-down"))
        "machine" -> machineMatches(n, kit)
        else -> false
    }
}

private val machineWords = listOf(
    "Leg press" to listOf("leg press"),
    "Leg extension" to listOf("leg extension"),
    "Leg curl" to listOf("leg curl", "hamstring curl"),
    "Chest press machine" to listOf("chest press", "machine bench press"),
    "Pec deck" to listOf("butterfly", "pec deck", "reverse machine fly", "machine fly"),
    "Shoulder press machine" to listOf("machine shoulder", "shoulder press"),
    "Hack squat" to listOf("hack squat"),
    "Calf raise machine" to listOf("calf raise", "calf press"),
    "Seated row machine" to listOf("seated row", "machine row", "lever row"),
    "Lat pulldown" to listOf("pulldown"),
    "Assisted pull-up machine" to listOf("assisted"),
    "Smith machine" to listOf("smith"),
    "Stationary bike" to listOf("bicycling, stationary", "recumbent bike"),
    "Treadmill" to listOf("treadmill", "walking, treadmill", "jogging, treadmill"),
    "Rowing machine" to listOf("rowing, stationary"),
    "Elliptical" to listOf("elliptical"),
)

private fun machineMatches(name: String, kit: Set<String>): Boolean =
    machineWords.any { (item, words) -> item in kit && words.any { it in name } }
