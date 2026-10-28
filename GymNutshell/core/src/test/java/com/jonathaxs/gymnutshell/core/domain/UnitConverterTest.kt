package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UnitConverterTest {

    @Test
    fun `onca liquida e passo de agua por locale`() {
        assertEquals(28.4131, UnitConverter.mlPerFlOz(Locale.UK), 1e-4)
        assertEquals(29.5735, UnitConverter.mlPerFlOz(Locale.US), 1e-4)
        assertEquals(10, UnitConverter.flOzWaterStep(Locale.UK))
        assertEquals(8, UnitConverter.flOzWaterStep(Locale.US))

        assertTrue(UnitConverter.usesImperialFlOz(Locale.UK))
        assertFalse(UnitConverter.usesImperialFlOz(Locale.US))
    }

    @Test
    fun `agua ml e flOz convertem e voltam em locale americano`() {
        // US: 29.5735 ml por fl oz
        assertEquals(16.907, UnitConverter.mlToFlOz(500.0, Locale.US), 1e-3)
        assertEquals(500.0, UnitConverter.flOzToMl(16.907, Locale.US), 1e-2)
    }

    @Test
    fun `agua ml e flOz convertem e voltam em locale britanico`() {
        // UK: 28.4131 ml por fl oz
        assertEquals(17.597, UnitConverter.mlToFlOz(500.0, Locale.UK), 1e-3)
        assertEquals(500.0, UnitConverter.flOzToMl(17.597, Locale.UK), 0.05)
    }
}
