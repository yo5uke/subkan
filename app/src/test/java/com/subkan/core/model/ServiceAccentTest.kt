package com.subkan.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceAccentTest {

    @Test
    fun `the same name always gets the same colour`() {
        assertEquals(ServiceAccent.forName("Netflix"), ServiceAccent.forName("Netflix"))
    }

    @Test
    fun `every name maps to a real slot, including empty and non-Latin ones`() {
        listOf("", "  ", "Netflix", "ネットフリックス", "🎬", "a".repeat(500)).forEach { name ->
            // forName indexes into the enum; a negative or out-of-range hash would throw here.
            ServiceAccent.forName(name)
        }
    }

    @Test
    fun `the initial is the first character, upper-cased`() {
        assertEquals("N", serviceInitial("netflix"))
        assertEquals("A", serviceInitial("  amazon prime  "))
        assertEquals("ネ", serviceInitial("ネットフリックス"))
    }

    @Test
    fun `a blank name falls back to a question mark rather than an empty tile`() {
        assertEquals("?", serviceInitial(""))
        assertEquals("?", serviceInitial("   "))
    }
}
