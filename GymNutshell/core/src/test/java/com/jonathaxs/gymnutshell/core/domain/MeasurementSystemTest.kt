package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MeasurementSystemTest {

    @Test
    fun `fromRaw mapeia uk para Metric e us para Us`() {
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.fromRaw("uk"))
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.fromRaw("metric"))
        assertEquals(MeasurementSystem.Us, MeasurementSystem.fromRaw("us"))
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.fromRaw(null))
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.fromRaw("desconhecido"))
    }

    @Test
    fun `detectDefault identifica Us apenas em paises do sistema americano`() {
        assertEquals(MeasurementSystem.Us, MeasurementSystem.detectDefault(Locale.US))
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.detectDefault(Locale.UK))
        assertEquals(MeasurementSystem.Metric, MeasurementSystem.detectDefault(Locale("pt", "BR")))
    }

    @Test
    fun `needsChoice retorna true para US e UK e false para Brasil`() {
        assertTrue(MeasurementSystem.needsChoice(Locale.US))
        assertTrue(MeasurementSystem.needsChoice(Locale.UK))
        assertFalse(MeasurementSystem.needsChoice(Locale("pt", "BR")))
    }
}
