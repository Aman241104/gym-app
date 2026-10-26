package app.gymapp.data

import app.gymapp.domain.Diet
import app.gymapp.domain.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/** Packaged food by barcode from Open Food Facts (open data, ODbL). Null if unknown or offline. */
object OpenFoodFacts {
    suspend fun lookup(barcode: String): Food? = withContext(Dispatchers.IO) {
        if (!barcode.all(Char::isDigit)) return@withContext null
        runCatching {
            val conn = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,nutriments").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "gym-app/0.1 (personal Android app)")
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            if (root["status"]?.jsonPrimitive?.content != "1") return@runCatching null
            val product = root["product"]!!.jsonObject
            val n = product["nutriments"]?.jsonObject ?: JsonObject(emptyMap())
            fun num(key: String) = n[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val name = listOfNotNull(product["brands"]?.jsonPrimitive?.content?.substringBefore(','), product["product_name"]?.jsonPrimitive?.content)
                .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Barcode $barcode" }
            // Diet isn't reliable in the data, so scanned foods aren't diet-filtered.
            Food("off_$barcode", name, "100 g", num("energy-kcal_100g"), num("proteins_100g"), num("carbohydrates_100g"), num("fat_100g"), Diet.Veg, jain = true, cooked = false)
        }.getOrNull()
    }
}
