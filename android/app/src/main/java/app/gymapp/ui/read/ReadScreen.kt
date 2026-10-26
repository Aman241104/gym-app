package app.gymapp.ui.read

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.gymapp.data.BookHit
import app.gymapp.data.Item
import app.gymapp.data.OpenLibrary
import app.gymapp.data.Repo
import app.gymapp.data.json
import app.gymapp.domain.Book
import app.gymapp.domain.pagesOn
import app.gymapp.domain.readingStreak
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.design.Accent
import app.gymapp.ui.design.CountUp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.util.UUID

/** ponytail: fixed daily goal; make it a setting if 20 pages ever feels wrong. */
private const val DAILY_PAGES = 20

private fun save(id: String, book: Book) = Repo.putItem("read", Item(id, json.encodeToJsonElement(Book.serializer(), book).jsonObject))

@Composable
fun ReadScreen(onClose: () -> Unit) {
    val amber = Accent.Read.color
    val all by Repo.items.collectAsState()
    val books = remember(all) {
        all["read"].orEmpty().mapNotNull { item -> runCatching { item.clientId to json.decodeFromJsonElement(Book.serializer(), item.data) }.getOrNull() }
    }
    val today = LocalDate.now()
    val shelf = books.map { it.second }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<BookHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<String, Book>?>(null) }
    var noting by remember { mutableStateOf<Pair<String, Book>?>(null) }

    fun search() {
        if (query.isBlank()) return
        searching = true
        scope.launch { hits = OpenLibrary.search(query.trim()); searching = false }
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ScreenHeader("Read", "Read", "daily", amber) }
                IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 20.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        item {
            val today_ = pagesOn(shelf, today)
            Panel(accent = amber) {
                Row(verticalAlignment = Alignment.Bottom) {
                    CountUp(today_, MaterialTheme.typography.displayMedium, amber)
                    Muted("  of $DAILY_PAGES pages today", Modifier.padding(bottom = 8.dp))
                }
                LinearProgressIndicator(
                    progress = { (today_ / DAILY_PAGES.toFloat()).coerceIn(0f, 1f) }, color = amber, trackColor = amber.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Muted("Streak ${readingStreak(shelf, today)} days · ${shelf.count { it.status == "done" }} books finished")
            }
        }
        val reading = books.filter { it.second.status == "reading" }
        if (reading.isNotEmpty()) item { Text("Reading", style = MaterialTheme.typography.titleLarge) }
        items(reading, key = { it.first }) { (id, b) ->
            Panel(onClick = { editing = id to b }) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Cover(b)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(b.title, style = MaterialTheme.typography.titleMedium)
                        if (b.author.isNotBlank()) Muted(b.author)
                        LinearProgressIndicator(progress = { b.progress }, color = amber, trackColor = amber.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                        Muted("Page ${b.currentPage} of ${b.pages} · tap to update")
                        TextButton(onClick = { noting = id to b }) { Text("Add a highlight (${b.highlights.size})", color = amber) }
                    }
                }
                b.highlights.lastOrNull()?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("Find a book") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = ::search, enabled = !searching) { Text(if (searching) "…" else "Search", color = amber) }
            }
        }
        items(hits, key = { "hit-${it.title}-${it.cover_i}" }) { h ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(Book(h.title, coverId = h.cover_i, pages = 1), small = true)
                Column(Modifier.weight(1f)) {
                    Text(h.title, style = MaterialTheme.typography.bodyLarge)
                    Muted(listOfNotNull(h.author_name.firstOrNull(), h.number_of_pages_median?.let { "$it pages" }).joinToString(" · "))
                }
                TextButton(onClick = {
                    save(UUID.randomUUID().toString(), Book(h.title, h.author_name.firstOrNull().orEmpty(), h.cover_i, h.number_of_pages_median ?: 250))
                    hits = emptyList()
                    query = ""
                }) { Text("Add", color = amber) }
            }
        }
        val done = books.filter { it.second.status == "done" }
        if (done.isNotEmpty()) item { Text("Finished", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp)) }
        items(done, key = { it.first }) { (id, b) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(b, small = true)
                Column(Modifier.weight(1f)) {
                    Text(b.title, style = MaterialTheme.typography.bodyLarge)
                    Muted("Finished ${b.finishedOn.orEmpty()} · ${b.highlights.size} highlights")
                }
                IconButton(onClick = { Repo.deleteItem("read", id) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${b.title}") }
            }
        }
        item { Muted("Book data and covers: Open Library.", Modifier.padding(top = 8.dp)) }
    }

    editing?.let { (id, b) ->
        var page by remember { mutableStateOf(b.currentPage.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(b.title) },
            text = {
                OutlinedTextField(
                    value = page, onValueChange = { page = it.filter(Char::isDigit) }, label = { Text("I'm on page (of ${b.pages})") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = { TextButton(onClick = { page.toIntOrNull()?.let { save(id, b.readTo(it, today)) }; editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { Repo.deleteItem("read", id); editing = null }) { Text("Remove book") } },
        )
    }
    noting?.let { (id, b) ->
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noting = null },
            title = { Text("Highlight") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("A line worth keeping") }, minLines = 3) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) save(id, b.copy(highlights = b.highlights + text.trim())); noting = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { noting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Cover(book: Book, small: Boolean = false) {
    val w = if (small) 44.dp else 72.dp
    Box(Modifier.width(w).height(w * 1.5f).clip(RoundedCornerShape(8.dp)).background(Accent.Read.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
        book.coverUrl?.let { AsyncImage(it, contentDescription = "${book.title} cover", contentScale = ContentScale.Crop, modifier = Modifier.size(w, w * 1.5f)) }
            ?: Text(book.title.take(1), style = MaterialTheme.typography.titleLarge, color = Accent.Read.color)
    }
}
