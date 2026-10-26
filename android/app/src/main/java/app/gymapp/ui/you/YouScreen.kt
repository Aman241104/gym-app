package app.gymapp.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.data.Server
import app.gymapp.domain.EquipmentGroups
import app.gymapp.domain.Goal
import app.gymapp.ui.eat.SetupPanel
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YouScreen() {
    val settings by Repo.settings.collectAsState()

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Settings", "Your", "setup") }
        item {
            Panel {
                Text("Equipment", style = MaterialTheme.typography.titleLarge)
                Muted("Tick everything you can use. Plans are built only from this.")
                EquipmentGroups.forEach { (group, items) ->
                    Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { item ->
                            val on = item in settings.equipment
                            FilterChip(
                                selected = on,
                                onClick = { Repo.updateSettings { s -> s.copy(equipment = if (on) s.equipment - item else s.equipment + item) } },
                                label = { Text(item) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Panel {
                Text("Units", style = MaterialTheme.typography.titleLarge)
                val units = listOf("kg", "lbs")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    units.forEachIndexed { i, unit ->
                        SegmentedButton(
                            selected = settings.unit == unit,
                            onClick = { Repo.updateSettings { it.copy(unit = unit) } },
                            shape = SegmentedButtonDefaults.itemShape(i, units.size),
                        ) { Text(unit) }
                    }
                }
            }
        }
        item {
            Panel {
                Text("Plan", style = MaterialTheme.typography.titleLarge)
                Muted("Days per week")
                val dayOptions = (2..6).toList()
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    dayOptions.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = settings.daysPerWeek == d,
                            onClick = { Repo.updateSettings { it.copy(daysPerWeek = d) } },
                            shape = SegmentedButtonDefaults.itemShape(i, dayOptions.size),
                        ) { Text("$d") }
                    }
                }
                Muted("Goal")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { g ->
                        FilterChip(
                            selected = settings.goal == g.name,
                            onClick = { Repo.updateSettings { it.copy(goal = g.name) } },
                            label = { Text(g.label) },
                        )
                    }
                }
                Muted("2-3 days: full body. 4: upper/lower. 5-6: push/pull/legs. Every 5th week is a lighter deload week.")
            }
        }
        item { SetupPanel(settings.dietType, settings.bodyWeightKg) }
        item { SyncPanel() }
    }
}

@Composable
private fun SyncPanel() {
    val server by Repo.server.collectAsState()
    val outbox by Repo.outbox.collectAsState()
    val error by Repo.syncError.collectAsState()
    val syncing by Repo.syncing.collectAsState()
    var url by rememberSaveable(server.url) { mutableStateOf(server.url) }
    var token by rememberSaveable(server.token) { mutableStateOf(server.token) }

    Panel {
        Text("Sync", style = MaterialTheme.typography.titleLarge)
        Muted(
            when {
                !server.configured -> "Not connected. Everything stays on this phone until you add your server."
                syncing -> "Syncing…"
                error != null -> "Couldn't sync: $error"
                outbox.isNotEmpty() -> "${outbox.size} change(s) waiting to sync"
                else -> "All synced"
            },
        )
        OutlinedTextField(
            value = url, onValueChange = { url = it }, label = { Text("Server URL") }, singleLine = true,
            placeholder = { Text("https://your-app.vercel.app") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token, onValueChange = { token = it }, label = { Text("App token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Repo.setServer(Server(url, token)) }, enabled = url != server.url || token != server.token) { Text("Save") }
            TextButton(onClick = { Repo.syncNow() }, enabled = server.configured && !syncing) { Text("Sync now") }
        }
    }
}
