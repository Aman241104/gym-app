package app.gymapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.gymapp.data.Repo
import app.gymapp.domain.WeekPlan
import app.gymapp.domain.buildPlan
import app.gymapp.domain.goalOf
import app.gymapp.domain.planWeek
import java.time.LocalDate

/** This week's plan from the current settings, or null until the exercise library has loaded. */
@Composable
fun rememberWeekPlan(today: LocalDate = LocalDate.now()): Pair<WeekPlan, Int>? {
    val library by Repo.library.collectAsState()
    val settings by Repo.settings.collectAsState()
    val swaps by Repo.swaps.collectAsState()
    val week = planWeek(settings.planStart, today)
    return remember(library, settings, swaps, week) {
        if (library.isEmpty()) null else buildPlan(library, settings.equipment, settings.daysPerWeek, goalOf(settings.goal), week, swaps) to week
    }
}
