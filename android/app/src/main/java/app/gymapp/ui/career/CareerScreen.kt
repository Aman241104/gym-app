package app.gymapp.ui.career

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gymapp.data.Item
import app.gymapp.data.Repo
import app.gymapp.data.json
import app.gymapp.domain.CareerDay
import app.gymapp.domain.Skill
import app.gymapp.domain.careerWeek
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.design.Accent
import app.gymapp.ui.design.CountUp
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.util.UUID

/** Your Job Search OS deployment; the full pipeline (scraping, CVs, tracking) lives there. */
private const val JOB_SEARCH_OS = "https://job-search-zeta-ten.vercel.app"

private fun saveDay(day: LocalDate, d: CareerDay) = Repo.putItem("career", Item("day-$day", json.encodeToJsonElement(CareerDay.serializer(), d).jsonObject))
private fun saveSkill(id: String, s: Skill) = Repo.putItem("career", Item(id, json.encodeToJsonElement(Skill.serializer(), s).jsonObject))

@Composable
fun CareerScreen(onClose: () -> Unit) {
    val teal = Accent.Career.color
    val context = LocalContext.current
    val all by Repo.items.collectAsState()
    val docs = all["career"].orEmpty()
    val today = LocalDate.now()
    val days = remember(docs) {
        docs.filter { it.clientId.startsWith("day-") }.mapNotNull { d ->
            runCatching { LocalDate.parse(d.clientId.removePrefix("day-")) to json.decodeFromJsonElement(CareerDay.serializer(), d.data) }.getOrNull()
        }.toMap()
    }
    val skills = remember(docs) {
        docs.filter { it.clientId.startsWith("skill-") }.mapNotNull { d -> runCatching { d.clientId to json.decodeFromJsonElement(Skill.serializer(), d.data) }.getOrNull() }
    }
    val todayDay = days[today] ?: CareerDay()
    val week = careerWeek(days, today)
    var newSkill by rememberSaveable { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ScreenHeader("Career", "Career", "focus", teal) }
                IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 20.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        item {
            Panel(accent = teal) {
                Text("Today", style = MaterialTheme.typography.titleLarge)
                Counter("Applications sent", todayDay.applied) { saveDay(today, todayDay.copy(applied = (todayDay.applied + it).coerceAtLeast(0))) }
                Counter("Follow-ups", todayDay.followUps) { saveDay(today, todayDay.copy(followUps = (todayDay.followUps + it).coerceAtLeast(0))) }
                Counter("Learning (min)", todayDay.learnMinutes, step = 15) { saveDay(today, todayDay.copy(learnMinutes = (todayDay.learnMinutes + it).coerceAtLeast(0))) }
            }
        }
        item {
            Panel {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Row {
                    Column(Modifier.weight(1f)) { CountUp(week.applied, MaterialTheme.typography.displaySmall, teal); Muted("applied") }
                    Column(Modifier.weight(1f)) { CountUp(week.followUps, MaterialTheme.typography.displaySmall, teal); Muted("follow-ups") }
                    Column(Modifier.weight(1f)) { CountUp(week.learnMinutes / 60, MaterialTheme.typography.displaySmall, teal, suffix = "h"); Muted("learning") }
                }
            }
        }
        item {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(JOB_SEARCH_OS))) },
                colors = ButtonDefaults.buttonColors(containerColor = teal),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                Text("  Open Job Search OS")
            }
        }
        item { Text("Skills", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        items(skills.sortedByDescending { it.second.minutes }, key = { it.first }) { (id, s) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.bodyLarge)
                    Muted("${s.minutes / 60}h ${s.minutes % 60}m total · ${s.log[today.toString()] ?: 0} min today")
                }
                TextButton(onClick = {
                    // Learning on a skill also counts toward today's learning minutes.
                    saveSkill(id, s.copy(log = s.log + (today.toString() to (s.log[today.toString()] ?: 0) + 15)))
                    saveDay(today, todayDay.copy(learnMinutes = todayDay.learnMinutes + 15))
                }) { Text("+15 min", color = teal) }
                IconButton(onClick = { Repo.deleteItem("career", id) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${s.name}") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newSkill, onValueChange = { newSkill = it }, label = { Text("Skill you're learning") }, singleLine = true, modifier = Modifier.weight(1f))
                TextButton(onClick = { saveSkill("skill-" + UUID.randomUUID(), Skill(newSkill.trim())); newSkill = "" }, enabled = newSkill.isNotBlank()) { Text("Add", color = teal) }
            }
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(-step) }) { Icon(Icons.Rounded.Remove, contentDescription = "Less $label") }
        Text("$value", style = MaterialTheme.typography.titleLarge, color = Accent.Career.color)
        IconButton(onClick = { onChange(step) }) { Icon(Icons.Rounded.Add, contentDescription = "More $label") }
    }
}
