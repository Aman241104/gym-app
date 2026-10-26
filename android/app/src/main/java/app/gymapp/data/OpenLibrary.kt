package app.gymapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
data class BookHit(
    val title: String,
    val author_name: List<String> = emptyList(),
    val cover_i: Int? = null,
    val number_of_pages_median: Int? = null,
)

@Serializable
private data class SearchResult(val docs: List<BookHit> = emptyList())

/** Open Library search (free, no key). Polite: one request per search, identifying User-Agent. */
object OpenLibrary {
    suspend fun search(query: String): List<BookHit> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(query, "UTF-8")
            val conn = URL("https://openlibrary.org/search.json?q=$q&limit=12&fields=title,author_name,cover_i,number_of_pages_median").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "gym-app/0.1 (personal Android app)")
            conn.connectTimeout = 8_000
            conn.readTimeout = 12_000
            json.decodeFromString(SearchResult.serializer(), conn.inputStream.bufferedReader().use { it.readText() }).docs
        }.getOrDefault(emptyList())
    }
}
