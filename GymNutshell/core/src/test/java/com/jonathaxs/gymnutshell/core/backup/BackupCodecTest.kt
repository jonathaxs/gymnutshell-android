package com.jonathaxs.gymnutshell.core.backup

import com.jonathaxs.gymnutshell.core.data.CustomGoalCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garante o round-trip do backup e a presença das chaves legadas (compat com o formato v4 do iOS). */
class BackupCodecTest {

    private fun samplePayload() = BackupPayload(
        version = BackupCodec.CURRENT_VERSION,
        exportedAt = "2026-06-08T00:00:00Z",
        profile = ProfileSnapshot(
            name = "Jonathas", height = 180, weight = 80.0, age = 30, sex = "male",
            userGoal = "maintenance", measurementSystem = "metric",
        ),
        goals = GoalsSnapshot(calories = 2900, sleep = 7, water = 3250, protein = 180, carbs = 320, goodFat = 80, fiber = 30),
        goalsOrder = emptyList(),
        customGoals = listOf(
            CustomGoalSnapshot(id = "3", emoji = "🧪", name = "Creatine", unit = "g", goal = 5, increment = 1, category = "Suplemento"),
        ),
        dailyRecords = listOf(
            RecordSnapshot(
                date = "2026-06-07T00:00:00Z", water = 3000, protein = 170, carbs = 300, goodFat = 70, fiber = 28,
                sleep = 7, percent = 92, achievementTitle = "Level 5", achievementEmoji = "🐉", points = 5,
                didWorkout = true, didCardio = false, customValues = mapOf("custom:3" to 5),
            ),
        ),
        appearance = AppearanceSnapshot(theme = "gym", accentColor = "blue"),
        preferences = PreferencesSnapshot(
            autoWorkoutCheckin = true,
            controlStyle = "stepper",
            notificationEnabled = mapOf("water" to true),
        ),
    )

    @Test
    fun `roundTrip preserva campos`() {
        val original = samplePayload()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `json usa chaves legadas do ios`() {
        val json = BackupCodec.encode(samplePayload())
        assertTrue("fitnessGoal", json.contains("\"fitnessGoal\""))
        assertTrue("fats (goals)", json.contains("\"fats\""))
        assertTrue("carb (record)", json.contains("\"carb\""))
        assertTrue("fat (record)", json.contains("\"fat\""))
        assertTrue("catTitle", json.contains("\"catTitle\""))
        assertTrue("catEmoji", json.contains("\"catEmoji\""))
    }

    @Test
    fun `encode omite opcionais nulos`() {
        // Sem appearance/preferences, esses campos não devem aparecer no JSON (espelha o nil do iOS).
        val minimal = samplePayload().copy(appearance = null, preferences = null)
        val json = BackupCodec.encode(minimal)
        assertTrue("sem appearance", !json.contains("\"appearance\""))
        assertTrue("sem preferences", !json.contains("\"preferences\""))
        // Mas os obrigatórios continuam, mesmo vazios.
        assertTrue("goalsOrder presente", json.contains("\"goalsOrder\""))
    }

    @Test
    fun `decode ignora campos ios only`() {
        // Um backup do iOS traz campos que o Android não modela; o decode deve ignorá-los sem quebrar.
        val iosJson = """
            {
              "version": 4,
              "exportedAt": "2026-06-08T00:00:00Z",
              "profile": {"name":"A","height":180,"weight":80.0,"age":30,"sex":"male","fitnessGoal":"maintenance"},
              "goals": {"calories":2900,"sleep":7,"water":3250,"protein":180,"carbs":320,"fats":80,"fiber":30},
              "goalsOrder": ["tracking.sleep"],
              "customGoals": [],
              "dailyRecords": [],
              "removedItems": ["x"],
              "customCategories": [{"id":"c1","name":"Foo"}],
              "categoryOrder": ["a"],
              "builtinCategoryOrder": ["Essencial"]
            }
        """.trimIndent()
        val decoded = BackupCodec.decode(iosJson)
        assertEquals("maintenance", decoded.profile.userGoal)
        assertEquals(80, decoded.goals.goodFat)
        assertNull(decoded.preferences)
    }

    @Test
    fun `estilo de controle viaja no backup e faz roundtrip`() {
        val original = samplePayload().copy(
            preferences = PreferencesSnapshot(controlStyle = "stepper"),
        )
        val json = BackupCodec.encode(original)
        assertTrue(json.contains("\"controlStyle\": \"stepper\"") || json.contains("\"controlStyle\""))
        val decoded = BackupCodec.decode(json)
        assertEquals("stepper", decoded.preferences?.controlStyle)
    }

    @Test
    fun `backup JSON sem controlStyle e sem symbolName decodifica com defaults`() {
        val json = """
            {
              "version": 4,
              "exportedAt": "2026-09-01T12:00:00Z",
              "profile": {"name":"Ana","height":170,"weight":65.5,"age":28,"sex":"other","fitnessGoal":"cutting"},
              "goals": {"calories":2000,"sleep":7,"water":3000,"protein":150,"carbs":300,"fats":80,"fiber":25},
              "goalsOrder": [],
              "customGoals": [],
              "dailyRecords": [],
              "customCategories": [{"id":"abc","name":"Alongamento","supportsRestDay":true}],
              "preferences": {"widgetBackground":"x","orientationLock":"portrait"}
            }
        """.trimIndent()
        val decoded = BackupCodec.decode(json)
        assertNull(decoded.preferences?.controlStyle)
        val cat = decoded.customCategories?.first()
        assertEquals("Alongamento", cat?.name)
        assertEquals(true, cat?.supportsRestDay)
        assertEquals("square.grid.2x2.fill", cat?.symbolName)
    }

    @Test
    fun `backup da 1 0 com dados fisicos ainda decodifica`() {
        val json = """
            {"version":4,"exportedAt":"2026-09-01T12:00:00Z",
             "profile":{"name":"Ana","height":170,"weight":65.5,"age":28,"sex":"other","fitnessGoal":"cutting"},
             "goals":{"calories":2000,"sleep":7,"water":3000,"protein":150,"carbs":300,"fats":80,"fiber":25},
             "goalsOrder":[],"customGoals":[],"dailyRecords":[]}
        """.trimIndent()
        val payload = BackupCodec.decode(json)
        assertEquals("Ana", payload.profile.name)
        assertEquals(2000, payload.goals.calories)
        assertEquals(170, payload.profile.height)
        assertEquals(65.5, payload.profile.weight, 1e-4)
        assertEquals(28, payload.profile.age)
    }

    @Test
    fun `categoria com symbolName personalizado viaja no json`() {
        val original = samplePayload().copy(
            customCategories = listOf(
                CustomGoalCategory(
                    id = "c1",
                    name = "Yoga",
                    supportsRestDay = false,
                    symbolName = "figure.yoga",
                ),
            ),
        )
        val json = BackupCodec.encode(original)
        assertTrue(json.contains("\"symbolName\": \"figure.yoga\"") || json.contains("\"symbolName\""))
        val decoded = BackupCodec.decode(json)
        assertEquals("figure.yoga", decoded.customCategories?.first()?.symbolName)
    }
}
