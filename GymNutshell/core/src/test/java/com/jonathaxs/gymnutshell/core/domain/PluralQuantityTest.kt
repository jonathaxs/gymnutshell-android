package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralQuantityTest {

    @Test
    fun `zero usa a forma do plural e os outros numeros ficam iguais`() {
        assertEquals(2, pluralQuantity(0))
        assertEquals(1, pluralQuantity(1))
        assertEquals(5, pluralQuantity(5))
    }
}
