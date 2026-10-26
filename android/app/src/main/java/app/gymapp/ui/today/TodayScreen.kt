package app.gymapp.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import app.gymapp.ui.design.Accent
import app.gymapp.ui.design.CountUp
import app.gymapp.ui.design.RingSpec
import app.gymapp.ui.design.Rings
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.domain.formatWeight
import app.gymapp.domain.localDate
import app.gymapp.domain.sessionsInLast7Days
import app.gymapp.domain.streak
import app.gymapp.domain.suggest
import app.gymapp.ui.Muted
import app.gymapp.ui.rememberWeekPlan
import app.gymapp.ui.train.Draft
import app.gymapp.domain.lastByExercise
import app.gymapp.domain.nextRun
import app.gymapp.domain.goalOf
import app.gymapp.domain.proteinTarget
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Spa
import app.gymapp.data.json
import app.gymapp.domain.Book
import app.gymapp.domain.SkinDay
import app.gymapp.domain.pagesOn
import app.gymapp.domain.runDays
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import java.time.Instant
import java.time.ZoneId
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(onStart: () -> Unit, onPosture: () -> Unit, onRun: () -> Unit, onEat: () -> Unit, onBreathe: () -> Unit, onRead: () -> Unit, onSkin: () -> Unit) {
    val items by Repo.items.collectAsState()
    val mind by Repo.mind.collectAsState()
    val food by Repo.food.collectAsState()
    val runs by Repo.runs.collectAsState()
    val postureDays by Repo.postureDays.collectAsState()
    val planState = rememberWeekPlan()
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val suggestions = workouts.maxByOrNull { it.date }?.exercises.orEmpty().mapNotNull { suggest(it, settings.unit) }

    val plan = planState?.first
    val liftDay = plan?.on(today.dayOfWeek)
    val runDay = plan != null && today.dayOfWeek in runDays(plan.days.map { it.day }.toSet())
    val liftDates = workouts.map { it.localDate() }.toSet()
    val runDates = runs.map { Instant.parse(it.date).atZone(zone).toLocalDate() }.toSet()
    val postureDates = postureDays.map(LocalDate::parse).toSet()
    val protein = food.filter { it.day == today.toString() }.sumOf { it.protein }
    val target = settings.bodyWeightKg?.let { proteinTarget(it, goalOf(settings.goal)) }

    val train = Accent.Train.color
    val run = Accent.Run.color
    val eat = Accent.Eat.color
    val posture = Accent.Posture.color

    // Today's four rings. Something not scheduled today counts as done, so rest days can close.
    data class Goal(val label: String, val color: Color, val progress: Float, val status: String)
    val goals = listOf(
        Goal("Train", train, if (liftDay == null || today in liftDates) 1f else 0f, if (liftDay == null) "Rest day" else if (today in liftDates) "Done" else liftDay.title),
        Goal("Run", run, if (!runDay || today in runDates) 1f else 0f, if (!runDay) "Not today" else if (today in runDates) "Done" else "To do"),
        Goal("Protein", eat, target?.let { (protein / it).toFloat() } ?: 0f, target?.let { "%.0f / %d g".format(protein, it) } ?: "Set weight"),
        Goal("Posture", posture, if (today in postureDates) 1f else 0f, if (today in postureDates) "Done" else "10 min"),
    )
    val closed = goals.count { it.progress >= 1f }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            val hour = LocalTime.now().hour
            ScreenHeader(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), "Good", if (hour < 12) "morning" else if (hour < 17) "afternoon" else "evening")
        }
        item {
            Panel(accent = train) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        Rings(goals.map { RingSpec(it.progress, it.color) }, Modifier.fillMaxSize(), stroke = 12.dp, gap = 4.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CountUp(closed, MaterialTheme.typography.headlineMedium, suffix = "/4")
                            Muted("closed")
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        goals.forEach { g ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(g.color))
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(g.label, style = MaterialTheme.typography.labelLarge)
                                    Muted(g.status)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { WeekStrip(today, liftDates, runDates, postureDates, train, run, posture) }
        item {
            val resuming = Draft.exercises.isNotEmpty()
            Panel(accent = train) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("UP NEXT", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp), color = train)
                        Text(
                            when {
                                resuming -> "Session in progress"
                                liftDay != null -> "${liftDay.title} day"
                                else -> "Rest and recover"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    planState?.let { (p, week) -> Muted("Week ${week + 1}" + if (p.deload) " · deload" else "") }
                }
                if (liftDay != null && !resuming) {
                    if (plan.deload) Muted("Lighter week: one set less on everything so you recover.")
                    liftDay.exercises.forEach { p ->
                        Row {
                            Text(p.exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Muted("${p.sets} × ${p.reps}")
                        }
                    }
                } else if (liftDay == null && !resuming) {
                    Muted("Do the posture block and an easy 20-30 minute walk. Or start a free workout.")
                }
                Button(
                    onClick = {
                        if (!resuming && liftDay != null) Draft.loadPlan(liftDay, lastByExercise(workouts), settings.unit)
                        onStart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = train),
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 6.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(
                        when {
                            resuming -> "  Resume session"
                            liftDay != null -> "  Start ${liftDay.title.lowercase()} day"
                            else -> "  Start a free workout"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                Tile(
                    "Posture", if (today in postureDates) "Done" else "10 min",
                    if (today in postureDates) "Streak ${streak(postureDates, today)}d" else "Chin tucks, chest, upper back",
                    if (today in postureDates) Icons.Rounded.CheckCircle else Icons.Rounded.SelfImprovement, posture, onPosture, Modifier.weight(1f).fillMaxHeight(),
                )
                Tile(
                    "Protein", "%.0f g".format(protein), target?.let { if (protein >= it) "Target hit" else "of $it g today" } ?: "Set your weight",
                    Icons.Rounded.Restaurant, eat, onEat, Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        item {
            // Life modules: one small tile each, today's number up front.
            val books = items["read"].orEmpty().mapNotNull { runCatching { json.decodeFromJsonElement(Book.serializer(), it.data) }.getOrNull() }
            val skinLog = items["skin"].orEmpty().find { it.clientId == "log-$today" }?.let { runCatching { json.decodeFromJsonElement(SkinDay.serializer(), it.data) }.getOrNull() }
            Text("LIFE", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min).padding(top = 8.dp)) {
                Tile("Mind", "${mind[today.toString()] ?: 0}", "min breathed", Icons.Rounded.Air, Accent.Mind.color, onBreathe, Modifier.weight(1f).fillMaxHeight())
                Tile("Read", "${pagesOn(books, today)}", "pages today", Icons.AutoMirrored.Rounded.MenuBook, Accent.Read.color, onRead, Modifier.weight(1f).fillMaxHeight())
                Tile("Skin", "${(skinLog?.am?.size ?: 0) + (skinLog?.pm?.size ?: 0)}", "steps done", Icons.Rounded.Spa, Accent.Skin.color, onSkin, Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (runDay) item {
            val next = nextRun(runs.filter { it.completed }.mapNotNull { it.plan }.toSet())
            Panel(accent = run, onClick = onRun) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RUN DAY", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp), color = run)
                        Text(if (today in runDates) "Run done" else "Week ${next.week}, run ${next.day}", style = MaterialTheme.typography.titleLarge)
                        Muted(if (today in runDates) "Nice. Stretch calves and hip flexors." else "${next.totalSeconds / 60} min · ${next.summary}")
                    }
                    Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = run, modifier = Modifier.size(34.dp))
                }
            }
        }
        item {
            Panel {
                Row {
                    BigStat(streak(workouts, today), "day streak", train, Modifier.weight(1f))
                    BigStat(sessionsInLast7Days(workouts, today), "lifts this week", train, Modifier.weight(1f))
                    BigStat(runs.count { ChronoUnit.DAYS.between(Instant.parse(it.date).atZone(zone).toLocalDate(), today) in 0..6 }, "runs this week", run, Modifier.weight(1f))
                }
            }
        }
        if (suggestions.isNotEmpty()) item {
            Panel {
                Text("Next time", style = MaterialTheme.typography.titleLarge)
                suggestions.forEach { s ->
                    Text("${s.exercise}  ·  ${formatWeight(s.weight)} ${settings.unit} × ${s.reps}", style = MaterialTheme.typography.titleSmall)
                    Muted(s.reason)
                }
            }
        }
        if (settings.equipment.isEmpty()) item {
            Panel {
                Text("What's in your gym?", style = MaterialTheme.typography.titleLarge)
                Muted("Tick your equipment on the You tab. Plans will only use what you have.")
            }
        }
    }
}

/** Mon-Sun, with a dot per thing done that day: lift, run, posture. Today is ringed. */
@Composable
private fun WeekStrip(today: LocalDate, lifts: Set<LocalDate>, runs: Set<LocalDate>, posture: Set<LocalDate>, train: Color, run: Color, posture2: Color) {
    val monday = today.with(java.time.DayOfWeek.MONDAY)
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { i ->
                val d = monday.plusDays(i.toLong())
                val isToday = d == today
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Muted(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .then(if (isToday) Modifier.border(2.dp, train, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = if (d.isAfter(today)) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(lifts to train, runs to run, posture to posture2).forEach { (set, color) ->
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (d in set) color else color.copy(alpha = 0.15f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, note: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    Panel(modifier, accent = accent, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp), color = accent, modifier = Modifier.padding(top = 8.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Muted(note)
    }
}

@Composable
private fun BigStat(value: Int, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        CountUp(value, MaterialTheme.typography.displaySmall, color)
        Muted(label)
    }
}
