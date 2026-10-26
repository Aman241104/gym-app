package app.gymapp.ui.eat

import app.gymapp.ui.design.Accent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.gymapp.data.FoodEntry
import app.gymapp.data.OpenFoodFacts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import app.gymapp.data.Repo
import app.gymapp.domain.Food
import app.gymapp.domain.fits
import app.gymapp.domain.goalOf
import app.gymapp.domain.proteinGapFixers
import app.gymapp.domain.proteinTarget
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val diets = listOf("veg" to "Veg", "egg" to "Eggetarian", "nonveg" to "Non-veg", "jain" to "Jain")

private fun fmt(v: Double) = if (v >= 10 || v % 1.0 == 0.0) "%.0f".format(v) else "%.1f".format(v)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EatScreen(scanSignal: Int = 0, onScanHandled: () -> Unit = {}) {
    val settings by Repo.settings.collectAsState()
    val foods by Repo.foods.collectAsState()
    val log by Repo.food.collectAsState()
    val today = LocalDate.now()
    val todays = log.filter { it.day == today.toString() }
    val protein = todays.sumOf { it.protein }
    val target = settings.bodyWeightKg?.let { proteinTarget(it, goalOf(settings.goal)) }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf<Food?>(null) }
    var scanNote by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun scan() {
        scanNote = null
        GmsBarcodeScanning.getClient(context).startScan().addOnSuccessListener { code ->
            val raw = code.rawValue ?: return@addOnSuccessListener
            scanNote = "Looking up $raw…"
            scope.launch {
                val found = OpenFoodFacts.lookup(raw)
                scanNote = if (found == null) "Not in Open Food Facts yet. Search by name instead." else null
                adding = found
            }
        }
    }
    LaunchedEffect(scanSignal) {
        if (scanSignal > 0) {
            onScanHandled()
            scan()
        }
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader(today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), "Eat", "well", Accent.Eat.color) }
        if (settings.dietType == null || settings.bodyWeightKg == null) item { SetupPanel(settings.dietType, settings.bodyWeightKg) }
        item {
            Panel(accent = Accent.Eat.color) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${fmt(protein)} g", style = MaterialTheme.typography.displayMedium, color = Accent.Eat.color)
                    Muted(target?.let { "  of $it g protein" } ?: "  protein", Modifier.padding(bottom = 6.dp))
                }
                if (target != null) LinearProgressIndicator(progress = { (protein / target).toFloat().coerceIn(0f, 1f) }, color = Accent.Eat.color, trackColor = Accent.Eat.color.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth().height(8.dp))
                Muted("${fmt(todays.sumOf { it.kcal })} kcal · ${fmt(todays.sumOf { it.carbs })} g carbs · ${fmt(todays.sumOf { it.fat })} g fat")
            }
        }
        val fixers = target?.let { proteinGapFixers(settings.dietType, it - protein) }.orEmpty()
        if (fixers.isNotEmpty()) item {
            Panel {
                Text("Close the protein gap", style = MaterialTheme.typography.titleMedium)
                Muted("${fmt(target!! - protein)} g to go. Quick adds that fit your diet:")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fixers.forEach { f -> AssistChip(onClick = { adding = f }, label = { Text("${f.name} +${fmt(f.protein)} g") }) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("Search food (roti, dal, paneer…)") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = { scan() }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Accent.Eat.color.copy(alpha = 0.22f), contentColor = Accent.Eat.color), modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan a barcode") }
            }
        }
        scanNote?.let { note -> item { Muted(note) } }
        val q = query.trim()
        if (q.isNotEmpty()) {
            val results = foods.filter { it.fits(settings.dietType) && it.name.contains(q, ignoreCase = true) }.take(25)
            if (results.isEmpty()) item { Muted("Nothing found for \"$q\".") }
            items(results, key = { "f-${it.id}" }) { f ->
                Row(Modifier.fillMaxWidth().clickable { adding = f }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.bodyLarge)
                        Muted("${f.unit} · ${fmt(f.protein)} g protein · ${fmt(f.kcal)} kcal" + if (f.cooked) "" else " · raw")
                    }
                    Icon(Icons.Rounded.Add, contentDescription = "Add ${f.name}")
                }
            }
        }
        item { Text("Today", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        if (todays.isEmpty()) item { Muted("Nothing logged yet today.") }
        items(todays, key = { it.clientId }) { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge)
                    Muted("${fmt(e.protein)} g protein · ${fmt(e.kcal)} kcal")
                }
                IconButton(onClick = { Repo.deleteFood(e) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${e.name}") }
            }
        }
        item { Muted("Cooked foods use approximate home-style portions (1 katori ≈ 150 ml). Raw foods: IFCT 2017 per 100 g. Packaged foods: Open Food Facts (ODbL).", Modifier.padding(top = 8.dp)) }
    }

    adding?.let { food -> AddDialog(food, onDismiss = { adding = null }) { servings ->
        val label = if (servings == 1.0) food.unit else "${fmt(servings)} × ${food.unit}"
        Repo.addFood(FoodEntry(UUID.randomUUID().toString(), today.toString(), "${food.name} ($label)", servings,
            food.kcal * servings, food.protein * servings, food.carbs * servings, food.fat * servings))
        adding = null
        query = ""
    } }
}

@Composable
private fun AddDialog(food: Food, onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var servings by remember { mutableDoubleStateOf(1.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { servings = (servings - 0.5).coerceAtLeast(0.5) }) { Icon(Icons.Rounded.Remove, contentDescription = "Less") }
                    Text("${fmt(servings)} × ${food.unit}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { servings += 0.5 }) { Icon(Icons.Rounded.Add, contentDescription = "More") }
                }
                Muted("${fmt(food.protein * servings)} g protein · ${fmt(food.kcal * servings)} kcal")
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(servings) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupPanel(dietType: String?, weight: Double?) {
    var kg by rememberSaveable { mutableStateOf(weight?.let { fmt(it) }.orEmpty()) }
    Panel {
        Text("Nutrition", style = MaterialTheme.typography.titleLarge)
        Muted("Your diet filters suggestions; your weight sets the daily protein target.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            diets.forEach { (value, label) ->
                FilterChip(
                    selected = dietType == value, onClick = { Repo.updateSettings { it.copy(dietType = value) } }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent.Eat.color.copy(alpha = 0.25f)),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = kg, onValueChange = { kg = it }, label = { Text("Weight (kg)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
            )
            val parsed = kg.replace(',', '.').toDoubleOrNull()?.takeIf { it in 25.0..300.0 }
            Button(onClick = { Repo.updateSettings { it.copy(bodyWeightKg = parsed) } }, enabled = parsed != null, colors = ButtonDefaults.buttonColors(containerColor = Accent.Eat.color)) { Text("Save") }
        }
    }
}
