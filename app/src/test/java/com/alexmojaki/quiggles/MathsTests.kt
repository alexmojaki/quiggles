package com.alexmojaki.quiggles

import org.junit.Test

import org.junit.Assert.*

class MathsTests {
    @Test
    fun star_isCorrect() {
        assertEquals(star(119.0), Pair(120.0, 3))
        assertEquals(star(120.0), Pair(120.0, 3))
        assertEquals(star(121.0), Pair(120.0, 3))

        assertEquals(star(10.0), Pair(40.0, 9))
        assertEquals(star(350.0), Pair(320.0, 9))

        assertEquals(star(90.0), Pair(90.0, 4))
        assertEquals(star(95.0), Pair(90.0, 4))
    }


}
