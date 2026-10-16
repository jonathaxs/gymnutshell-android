package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControlStyleTest {

    @Test
    fun `fromRaw tem slider como default`() {
        assertEquals(ControlStyle.Slider, ControlStyle.fromRaw("slider"))
        assertEquals(ControlStyle.Stepper, ControlStyle.fromRaw("stepper"))
        assertEquals(ControlStyle.Slider, ControlStyle.fromRaw(null))
        assertEquals(ControlStyle.Slider, ControlStyle.fromRaw("x"))
    }

    @Test
    fun `fromRawOrNull devolve null para valores desconhecidos`() {
        assertNull(ControlStyle.fromRawOrNull("x"))
        assertNull(ControlStyle.fromRawOrNull(null))
        assertNull(ControlStyle.fromRawOrNull(""))
        assertEquals(ControlStyle.Slider, ControlStyle.fromRawOrNull("slider"))
        assertEquals(ControlStyle.Stepper, ControlStyle.fromRawOrNull("stepper"))
    }
}
