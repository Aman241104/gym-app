package app.gymapp.ui.skin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gymapp.data.Item
import app.gymapp.data.Repo
import app.gymapp.data.json
import app.gymapp.domain.SkinDay
import app.gymapp.domain.SkinProduct
import app.gymapp.domain.SkinTypes
import app.gymapp.domain.routine
import app.gymapp.domain.skinWarnings
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.design.Accent
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinScreen(onClose: () -> Unit) {
    val coral = Accent.Skin.color
    val all by Repo.items.collectAsState()
    val today = LocalDate.now()
    val docs = all["skin"].orEmpty()
    // Products are "p-…" items; each day's ticks are one "log-yyyy-mm-dd" item.
    val products = remember(docs) { docs.filter { it.clientId.startsWith("p-") }.mapNotNull { d -> runCatching { d.clientId to json.decodeFromJsonElement(SkinProduct.serializer(), d.data) }.getOrNull() } }
    val logId = "log-$today"
    val day = remember(docs) { docs.find { it.clientId == logId }?.let { runCatching { json.decodeFromJsonElement(SkinDay.serializer(), it.data) }.getOrNull() } ?: SkinDay() }
    var slot by rememberSaveable { mutableStateOf(if (LocalTime.now().hour < 15) "AM" else "PM") }
    var adding by rememberSaveable { mutableStateOf(false) }
    val shelf = products.map { it.second }
    val steps = routine(shelf, slot)
    val ticked = if (slot == "AM") day.am else day.pm

    fun toggle(id: String) {
        val list = if (id in ticked) ticked - id else ticked + id
        val next = if (slot == "AM") day.copy(am = list) else day.copy(pm = list)
        Repo.putItem("skin", Item(logId, json.encodeToJsonElement(SkinDay.serializer(), next).jsonObject))
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ScreenHeader("Skin", "Skin", "care", coral) }
                IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 20.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("AM", "PM").forEachIndexed { i, s ->
                    SegmentedButton(selected = slot == s, onClick = { slot = s }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(if (s == "AM") "Morning" else "Night") }
                }
            }
        }
        val warnings = skinWarnings(shelf, slot)
        if (warnings.isNotEmpty()) item {
            Panel(accent = coral) {
                warnings.forEach { w ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = coral, modifier = Modifier.size(20.dp))
                        Text(w, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Muted("General reminders, not medical advice.")
            }
        }
        item {
            Panel(accent = coral) {
                val idsInRoutine = products.filter { it.second in steps }.map { it.first }
                Text("${ticked.count { it in idsInRoutine }} of ${steps.size} done", style = MaterialTheme.typography.titleLarge)
                if (steps.isEmpty()) Muted("Add your products below to build your ${if (slot == "AM") "morning" else "night"} routine.")
                steps.forEachIndexed { i, p ->
                    val id = products.first { it.second == p }.first
                    val done = id in ticked
                    Row(Modifier.fillMaxWidth().clickable { toggle(id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Muted("${i + 1}", Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Muted(p.type + if (p.expired(today)) " · past its open life" else "")
                        }
                        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = if (done) "Done" else "Not done", tint = if (done) coral else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        item { Text("Shelf", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        items(products.sortedBy { SkinTypes.indexOf(it.second.type) }, key = { it.first }) { (id, p) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    Muted(listOfNotNull(p.type, if (p.whenUsed == "Both") "AM + PM" else p.whenUsed, p.openedOn?.let { "opened $it" }, if (p.expired(today)) "replace soon" else null).joinToString(" · "))
                }
                IconButton(onClick = { Repo.deleteItem("skin", id) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${p.name}") }
            }
        }
        item {
            if (!adding) {
                Button(onClick = { adding = true }, colors = ButtonDefaults.buttonColors(containerColor = coral), modifier = Modifier.fillMaxWidth()) { Text("Add a product") }
            } else {
                AddProduct(onDone = { adding = false })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProduct(onDone: () -> Unit) {
    val coral = Accent.Skin.color
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Moisturiser") }
    var whenUsed by rememberSaveable { mutableStateOf("Both") }
    var pao by rememberSaveable { mutableStateOf<Int?>(12) }
    val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = coral.copy(alpha = 0.25f))
    Panel(accent = coral) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Muted("Type")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SkinTypes.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }, colors = chip) } }
        Muted("When")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("AM", "PM", "Both").forEach { w -> FilterChip(selected = whenUsed == w, onClick = { whenUsed = w }, label = { Text(w) }, colors = chip) } }
        Muted("Opened today; use within")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6, 12, 24).forEach { m -> FilterChip(selected = pao == m, onClick = { pao = m }, label = { Text("$m months") }, colors = chip) }
            FilterChip(selected = pao == null, onClick = { pao = null }, label = { Text("Not opened") }, colors = chip)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            Button(
                onClick = {
                    val p = SkinProduct(name.trim(), type, whenUsed, if (pao != null) LocalDate.now().toString() else null, pao)
                    Repo.putItem("skin", Item("p-" + UUID.randomUUID(), json.encodeToJsonElement(SkinProduct.serializer(), p).jsonObject))
                    onDone()
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = coral),
            ) { Text("Save") }
            androidx.compose.material3.TextButton(onClick = onDone) { Text("Cancel") }
        }
    }
}
