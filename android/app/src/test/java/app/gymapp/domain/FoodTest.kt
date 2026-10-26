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
