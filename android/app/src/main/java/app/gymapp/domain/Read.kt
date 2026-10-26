package app.gymapp.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** A book on the shelf. [log] is pages read per local date; status is "want", "reading" or "done". */
@Serializable
data class Book(
    val title: String,
    val author: String = "",
    val coverId: Int? = null,
    val pages: Int,
    val status: String = "reading",
    val currentPage: Int = 0,
    val log: Map<String, Int> = emptyMap(),
    val highlights: List<String> = emptyList(),
    val finishedOn: String? = null,
) {
    val coverUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
    val progress: Float get() = if (pages <= 0) 0f else (currentPage.toFloat() / pages).coerceIn(0f, 1f)

    /** Moves the bookmark to [page]; pages gained count toward [day]. Reaching the end finishes the book. */
    fun readTo(page: Int, day: LocalDate): Book {
        val target = page.coerceIn(0, pages)
        val gained = (target - currentPage).coerceAtLeast(0)
        val done = target >= pages
        return copy(
            currentPage = target,
            log = if (gained > 0) log + (day.toString() to (log[day.toString()] ?: 0) + gained) else log,
            status = if (done) "done" else if (status == "want") "reading" else status,
            finishedOn = if (done) (finishedOn ?: day.toString()) else finishedOn,
        )
    }
}

fun pagesOn(books: List<Book>, day: LocalDate): Int = books.sumOf { it.log[day.toString()] ?: 0 }

/** Consecutive days with any pages read, ending today or yesterday. */
fun readingStreak(books: List<Book>, today: LocalDate): Int =
    streak(books.flatMap { b -> b.log.filterValues { it > 0 }.keys }.map(LocalDate::parse).toSet(), today)
