package app.gymapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoodTest {
    private val ifct = parseIfct(File("src/main/assets/ifct.json").readText())

    @Test fun dietFiltersAndJain() {
        assertTrue(ifct.size > 400)
        assertTrue(IndianFoods.filter { it.fits("veg") }.none { it.diet != Diet.Veg })
        assertTrue(IndianFoods.filter { it.fits("jain") }.none { "aloo" in it.id || it.diet != Diet.Veg })
        assertTrue(ifct.filter { it.fits("jain") }.none { "potato" in it.name.lowercase() || "onion" in it.name.lowercase() })
        assertTrue(ifct.first { it.name == "Paneer" }.protein in 17.0..20.0)
    }

    @Test fun proteinTargetAndGap() {
        assertEquals(112, proteinTarget(70.0, Goal.Muscle))
        assertTrue(proteinGapFixers("veg", 40.0).all { it.diet == Diet.Veg && it.protein >= 6 })
        assertEquals("whey", proteinGapFixers("veg", 40.0).first().id)
        assertTrue(proteinGapFixers("veg", 3.0).isEmpty())
    }
}

class ReadTest {
    @org.junit.Test fun readingProgressAndStreak() {
        val d1 = java.time.LocalDate.of(2026, 10, 26)
        val d2 = d1.plusDays(1)
        val b = Book("Atomic Habits", pages = 100, status = "want").readTo(30, d1).readTo(25, d2).readTo(60, d2)
        org.junit.Assert.assertEquals("reading", b.status)
        org.junit.Assert.assertEquals(mapOf("2026-10-26" to 30, "2026-10-27" to 35), b.log) // going back never subtracts
        org.junit.Assert.assertEquals(35, pagesOn(listOf(b), d2))
        org.junit.Assert.assertEquals(2, readingStreak(listOf(b), d2))
        val done = b.readTo(500, d2)
        org.junit.Assert.assertEquals("done", done.status)
        org.junit.Assert.assertEquals(100, done.currentPage)
    }
}

class SkinTest {
    @org.junit.Test fun clashesOrderAndExpiry() {
        val p = listOf(
            SkinProduct("Moisturiser", "Moisturiser"), SkinProduct("Retinal", "Retinoid", "PM"),
            SkinProduct("BHA", "AHA/BHA exfoliant", "PM"), SkinProduct("Cleanser", "Cleanser"),
        )
        org.junit.Assert.assertEquals(listOf("Cleanser", "AHA/BHA exfoliant", "Retinoid", "Moisturiser"), routine(p, "PM").map { it.type })
        org.junit.Assert.assertEquals(1, skinWarnings(p, "PM").size)
        org.junit.Assert.assertTrue(skinWarnings(p, "AM").single().contains("sunscreen"))
        val today = java.time.LocalDate.of(2026, 10, 27)
        org.junit.Assert.assertTrue(SkinProduct("Old", "Other", openedOn = "2026-01-01", paoMonths = 6).expired(today))
        org.junit.Assert.assertFalse(SkinProduct("New", "Other", openedOn = "2026-10-01", paoMonths = 6).expired(today))
    }
}
