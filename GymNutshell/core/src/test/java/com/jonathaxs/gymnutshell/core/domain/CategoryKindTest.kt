package com.jonathaxs.gymnutshell.core.domain

import com.jonathaxs.gymnutshell.core.data.CustomGoal
import com.jonathaxs.gymnutshell.core.data.CustomGoalCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryKindTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun `categorias fixas tem tipo fixo`() {
        assertEquals(CategoryKind.RestDay, GoalCategory.Treino.kind)
        assertEquals(CategoryKind.CheckIn, GoalCategory.Suplemento.kind)
        assertEquals(CategoryKind.Standard, GoalCategory.Essencial.kind)
        assertEquals(CategoryKind.Standard, GoalCategory.Nutricao.kind)
    }

    @Test
    fun `categoria sem kind no json vira tipo pelo descanso`() {
        val comDescanso = json.decodeFromString<CustomGoalCategory>("""{"id":"a","name":"Corrida","supportsRestDay":true}""")
        val semDescanso = json.decodeFromString<CustomGoalCategory>("""{"id":"b","name":"Leitura"}""")
        assertEquals(CategoryKind.RestDay, comDescanso.kind)
        assertEquals(CategoryKind.Standard, semDescanso.kind)
    }

    @Test
    fun `json do iOS com checkIn e tipo desconhecido`() {
        val checkIn = json.decodeFromString<CustomGoalCategory>("""{"id":"c","name":"Vitaminas","kind":"checkIn","supportsRestDay":false}""")
        val futuro = json.decodeFromString<CustomGoalCategory>("""{"id":"d","name":"X","kind":"futuro","supportsRestDay":false}""")
        assertEquals(CategoryKind.CheckIn, checkIn.kind)
        assertEquals(CategoryKind.Standard, futuro.kind)
    }

    @Test
    fun `check-in viaja no json com o rawValue do iOS`() {
        val cat = CustomGoalCategory(name = "Vitaminas").withKind(CategoryKind.CheckIn)
        val text = json.encodeToString(CustomGoalCategory.serializer(), cat)
        assertTrue(text.contains("\"kind\":\"checkIn\""))
        assertEquals(CategoryKind.CheckIn, json.decodeFromString<CustomGoalCategory>(text).kind)
    }

    @Test
    fun `withKind mantem supportsRestDay coerente`() {
        val cat = CustomGoalCategory(name = "Meditar")
        assertTrue(cat.withKind(CategoryKind.RestDay).supportsRestDay)
        assertFalse(cat.withKind(CategoryKind.RestDay).withKind(CategoryKind.CheckIn).supportsRestDay)
    }

    @Test
    fun `meta herda o tipo da categoria`() {
        val vitaminas = CustomGoalCategory(id = "v", name = "Vitaminas").withKind(CategoryKind.CheckIn)
        fun meta(raw: String? = null, customId: String? = null) =
            CustomGoal(emoji = "💊", name = "X", unit = "mg", target = 1, increment = 1, categoryRaw = raw, customCategoryId = customId)
        assertEquals(CategoryKind.CheckIn, meta(raw = "suplemento").kind(emptyList()))
        assertEquals(CategoryKind.CheckIn, meta(customId = "v").kind(listOf(vitaminas)))
        assertEquals(CategoryKind.Standard, meta().kind(listOf(vitaminas)))
        assertEquals(CategoryKind.Standard, meta(customId = "sumiu").kind(listOf(vitaminas)))
    }

    @Test
    fun `check-in grava meta ou zero`() {
        assertEquals(5, CategoryKind.checkInValue(done = true, goal = 5))
        assertEquals(0, CategoryKind.checkInValue(done = false, goal = 5))
        assertTrue(CategoryKind.isCheckInDone(current = 5, goal = 5))
        assertFalse(CategoryKind.isCheckInDone(current = 3, goal = 5))
        assertFalse(CategoryKind.isCheckInDone(current = 0, goal = 0))
    }
}
