package app.gymapp.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class Diet { Veg, Egg, NonVeg }

/** Nutrients per one [unit] ("1 roti", "1 katori", "100 g"). */
data class Food(
    val id: String,
    val name: String,
    val unit: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val diet: Diet,
    /** No meat/egg and no root vegetables, onion or garlic. */
    val jain: Boolean,
    val cooked: Boolean,
)

private fun f(id: String, name: String, unit: String, kcal: Int, p: Double, c: Double, fat: Double, diet: Diet = Diet.Veg, jain: Boolean = true) =
    Food(id, name, unit, kcal.toDouble(), p, c, fat, diet, jain, cooked = true)

/**
 * Common home-cooked Indian foods in the units people actually eat them in. Values are
 * approximate, home-style portions (katori ≈ 150 ml), written for this app, not from a database.
 */
val IndianFoods: List<Food> = listOf(
    f("roti", "Roti / chapati", "1 roti", 110, 3.5, 20.0, 2.5),
    f("paratha", "Plain paratha", "1 paratha", 200, 4.0, 26.0, 9.0),
    f("aloo_paratha", "Aloo paratha", "1 paratha", 290, 6.0, 40.0, 12.0, jain = false),
    f("rice", "Steamed rice", "1 katori", 195, 4.0, 42.0, 0.5),
    f("jeera_rice", "Jeera rice", "1 katori", 230, 4.0, 42.0, 5.0),
    f("khichdi", "Moong dal khichdi", "1 katori", 200, 7.0, 33.0, 4.0),
    f("dal", "Dal (toor / moong), plain", "1 katori", 150, 9.0, 22.0, 3.0),
    f("dal_tadka", "Dal tadka", "1 katori", 180, 9.0, 22.0, 6.0, jain = false),
    f("rajma", "Rajma curry", "1 katori", 210, 10.0, 28.0, 6.0, jain = false),
    f("chole", "Chole", "1 katori", 240, 11.0, 30.0, 8.0, jain = false),
    f("sprouts", "Moong sprouts", "1 katori", 100, 7.0, 15.0, 0.5, jain = false),
    f("soya_chunks", "Soya chunks curry", "1 katori", 160, 16.0, 12.0, 5.0, jain = false),
    f("paneer", "Paneer", "100 g", 265, 18.0, 3.5, 20.0),
    f("paneer_bhurji", "Paneer bhurji", "1 katori", 320, 18.0, 6.0, 25.0, jain = false),
    f("palak_paneer", "Palak paneer", "1 katori", 280, 13.0, 9.0, 21.0, jain = false),
    f("tofu", "Tofu", "100 g", 80, 8.0, 2.0, 4.5),
    f("mixed_sabzi", "Mixed veg sabzi", "1 katori", 130, 3.0, 14.0, 7.0, jain = false),
    f("bhindi", "Bhindi sabzi", "1 katori", 120, 3.0, 10.0, 8.0),
    f("curd", "Curd / dahi", "1 katori", 90, 5.0, 7.0, 4.5),
    f("hung_curd", "Hung curd / Greek yogurt", "100 g", 100, 9.0, 4.0, 5.0),
    f("milk", "Milk (toned)", "1 glass", 150, 8.0, 12.0, 7.5),
    f("chaas", "Chaas / buttermilk", "1 glass", 40, 2.0, 4.0, 1.0),
    f("whey", "Whey protein", "1 scoop", 120, 24.0, 3.0, 1.5),
    f("poha", "Poha", "1 plate", 250, 5.0, 45.0, 6.0, jain = false),
    f("upma", "Upma", "1 plate", 250, 6.0, 38.0, 8.0, jain = false),
    f("idli", "Idli", "2 idli", 140, 4.0, 28.0, 0.6),
    f("dosa", "Plain dosa", "1 dosa", 170, 4.0, 28.0, 5.0),
    f("masala_dosa", "Masala dosa", "1 dosa", 300, 6.0, 45.0, 11.0, jain = false),
    f("sambar", "Sambar", "1 katori", 120, 5.0, 17.0, 3.0, jain = false),
    f("chilla", "Besan chilla", "2 chilla", 220, 11.0, 28.0, 7.0),
    f("dhokla", "Dhokla", "4 pieces", 160, 6.0, 25.0, 4.0),
    f("oats", "Oats with milk", "1 bowl", 260, 11.0, 38.0, 7.0),
    f("bread", "Bread", "1 slice", 75, 2.5, 14.0, 1.0),
    f("peanut_butter", "Peanut butter", "1 tbsp", 95, 4.0, 3.0, 8.0),
    f("peanuts", "Peanuts", "1 handful (30 g)", 170, 7.5, 5.0, 14.0),
    f("almonds", "Almonds", "10 almonds", 70, 2.5, 2.5, 6.0),
    f("roasted_chana", "Roasted chana", "1 handful (30 g)", 110, 6.5, 18.0, 1.8),
    f("banana", "Banana", "1 medium", 105, 1.3, 27.0, 0.4),
    f("apple", "Apple", "1 medium", 95, 0.5, 25.0, 0.3),
    f("tea", "Chai with milk and sugar", "1 cup", 60, 1.5, 9.0, 2.0),
    f("ghee", "Ghee", "1 tsp", 45, 0.0, 0.0, 5.0),
    f("samosa", "Samosa", "1 samosa", 260, 4.0, 30.0, 14.0, jain = false),
    f("boiled_egg", "Boiled egg", "1 egg", 78, 6.3, 0.6, 5.3, Diet.Egg, jain = false),
    f("omelette", "Omelette (2 eggs)", "1 omelette", 190, 12.0, 2.0, 15.0, Diet.Egg, jain = false),
    f("egg_bhurji", "Egg bhurji (2 eggs)", "1 plate", 210, 13.0, 4.0, 16.0, Diet.Egg, jain = false),
    f("egg_curry", "Egg curry (2 eggs)", "1 katori", 280, 14.0, 8.0, 21.0, Diet.Egg, jain = false),
    f("chicken_curry", "Chicken curry", "1 katori", 260, 22.0, 6.0, 16.0, Diet.NonVeg, jain = false),
    f("chicken_breast", "Grilled chicken breast", "100 g", 165, 31.0, 0.0, 3.6, Diet.NonVeg, jain = false),
    f("tandoori_chicken", "Tandoori chicken", "2 pieces", 260, 30.0, 4.0, 13.0, Diet.NonVeg, jain = false),
    f("fish_curry", "Fish curry", "1 katori", 220, 20.0, 6.0, 12.0, Diet.NonVeg, jain = false),
    f("mutton_curry", "Mutton curry", "1 katori", 320, 22.0, 6.0, 23.0, Diet.NonVeg, jain = false),
)

@Serializable
private data class IfctRow(val id: String, val name: String, val group: String, val diet: String, val kcal: Double, val protein: Double, val carbs: Double, val fat: Double)

private val nonJainWords = listOf("onion", "garlic", "potato", "carrot", "beet", "radish", "ginger", "turnip", "yam", "colocasia", "tapioca")

/** IFCT 2017 raw foods (MIT, github.com/nodef/ifct2017), per 100 g. Rows missing energy data are dropped. */
fun parseIfct(text: String): List<Food> =
    Json.decodeFromString(ListSerializer(IfctRow.serializer()), text).filter { it.kcal > 0 }.map { r ->
        val diet = when (r.diet) { "veg" -> Diet.Veg; "egg" -> Diet.Egg; else -> Diet.NonVeg }
        val jain = diet == Diet.Veg && r.group != "Roots and Tubers" && nonJainWords.none { it in r.name.lowercase() }
        Food("ifct_${r.id}", r.name, "100 g", r.kcal, r.protein, r.carbs, r.fat, diet, jain, cooked = false)
    }

/** Diet settings value → whether [food] fits. Null (not chosen yet) allows everything. */
fun Food.fits(dietType: String?): Boolean = when (dietType) {
    "jain" -> jain
    "veg" -> diet == Diet.Veg
    "egg" -> diet != Diet.NonVeg
    else -> true
}

/** Grams of protein per kg body weight per day: more while cutting to keep muscle. */
fun proteinTarget(bodyWeightKg: Double, goal: Goal): Int = (bodyWeightKg * when (goal) {
    Goal.FatLoss -> 1.8
    Goal.Muscle, Goal.Strength -> 1.6
    Goal.General -> 1.2
}).toInt()

/** Cooked foods that fit the diet, best protein per calorie first, to close today's protein gap. */
fun proteinGapFixers(dietType: String?, gramsLeft: Double): List<Food> =
    if (gramsLeft <= 5) emptyList()
    else IndianFoods.filter { it.fits(dietType) && it.protein >= 6 }.sortedByDescending { it.protein / it.kcal }.take(5)
