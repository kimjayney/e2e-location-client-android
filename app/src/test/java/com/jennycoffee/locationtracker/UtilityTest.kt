package com.jennycoffee.locationtracker

import org.junit.Test
import org.junit.Assert.*

class UtilityTest {
    
    @Test
    fun `test basic arithmetic`() {
        assertEquals(4, 2 + 2)
        assertEquals(10, 5 * 2)
        assertEquals(3, 9 / 3)
    }
    
    @Test
    fun `test string operations`() {
        val testString = "Hello World"
        assertTrue(testString.contains("Hello"))
        assertTrue(testString.contains("World"))
        assertEquals(11, testString.length)
    }
    
    @Test
    fun `test list operations`() {
        val testList = listOf(1, 2, 3, 4, 5)
        assertEquals(5, testList.size)
        assertTrue(testList.contains(3))
        assertEquals(15, testList.sum())
    }
    
    @Test
    fun `test regex pattern`() {
        val pattern = Regex("[a-zA-Z0-9]+")
        assertTrue(pattern.matches("abc123"))
        assertTrue(pattern.matches("XYZ789"))
        assertFalse(pattern.matches("abc-123"))
        assertFalse(pattern.matches("abc 123"))
    }
}
