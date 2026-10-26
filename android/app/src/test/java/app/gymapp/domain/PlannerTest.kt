package app.gymapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlannerTest {
    private val library = parseLibrary(File("src/main/assets/exercises.json").readText())
    private val ids = library.map { it.id }.toSet()

    private fun dump(label: String, plan: WeekPlan) {
        println("== $label (deload=${plan.deload})")
        plan.days.forEach { d -> println("${d.day} ${d.title}: " + d.exercises.joinToString { "${it.slot.key}=${it.exercise.name} ${it.sets}x${it.reps}" }) }
    }

    @Test fun preferredIdsExist() {
        val plan = buildPlan(library, EquipmentGroups.flatMap { it.second }, 6, Goal.Muscle, 0)
        plan.days.flatMap { it.exercises }.forEach { assertTrue(it.exercise.id in ids) }
        val missing = plan.days.flatMap { it.exercises }.flatMap { it.slot.prefer }.toSet() - ids
        assertTrue("missing $missing", missing.isEmpty())
    }

    @Test fun respectsEquipment() {
        val kit = listOf("Dumbbells", "Seated row machine", "Pull-up bar")
        val plan = buildPlan(library, kit, 4, Goal.Muscle, 0)
        dump("dumbbells + row machine + bar, 4 days", plan)
        val all = plan.days.flatMap { it.exercises }.map { it.exercise }
        assertTrue(all.all { it.doableWith(kit.toSet()) })
        assertTrue(all.none { "bench" in it.name.lowercase() })
        assertEquals("Seated_Cable_Rows", plan.days[0].exercises.first { it.slot.key == "h_pull" }.exercise.id)
        assertTrue(plan.days.filter { it.title == "Upper" }.all { d -> d.exercises.any { it.slot.key == "rear_delt" } })
    }

    @Test fun bodyweightOnlyStillPlans() {
        val plan = buildPlan(library, emptyList(), 3, Goal.General, 0)
        dump("no equipment, 3 days", plan)
        assertTrue(plan.days.all { it.exercises.size >= 4 })
        assertFalse(plan.days.flatMap { it.exercises }.any { it.exercise.equipment !in setOf("body only", null) })
    }

    @Test fun deloadAndSwap() {
        val kit = listOf("Dumbbells", "Flat bench")
        val normal = buildPlan(library, kit, 3, Goal.Strength, 0)
        val deload = buildPlan(library, kit, 3, Goal.Strength, 4)
        assertEquals(normal.days[0].exercises[0].sets - 1, deload.days[0].exercises[0].sets)
        val swapped = buildPlan(library, kit, 3, Goal.Strength, 0, mapOf("h_push" to 1))
        assertTrue(normal.days[0].exercises.first { it.slot.key == "h_push" }.exercise != swapped.days[0].exercises.first { it.slot.key == "h_push" }.exercise)
    }

    @Test fun muscleBalanceCountsPrimaryAndSecondary() {
        val today = java.time.LocalDate.of(2026, 10, 27)
        val at = today.atTime(18, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString()
        val old = today.minusDays(9).atTime(18, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toString()
        val row = app.gymapp.data.Exercise("Seated Cable Rows", List(3) { app.gymapp.data.SetEntry(10, 40.0) })
        val balance = muscleBalance(listOf(app.gymapp.data.Workout(date = at, exercises = listOf(row)), app.gymapp.data.Workout(date = old, exercises = listOf(row))), library, today).toMap()
        assertEquals(4.5, balance["Upper back"]!!, 0.01) // 3 middle back (primary) + 1.5 lats (secondary); old workout ignored
        assertEquals(0.0, balance["Quads"]!!, 0.0)
    }
}
