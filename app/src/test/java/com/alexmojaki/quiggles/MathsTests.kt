package com.alexmojaki.quiggles

import org.junit.Test

import org.junit.Assert.*

class MathsTests {
    @Test
    fun star_isCorrect() {
        assertEquals(star(119f), Pair(120f, 3))
        assertEquals(star(120f), Pair(120f, 3))
        assertEquals(star(121f), Pair(120f, 3))

        assertEquals(star(10f), Pair(40f, 9))
        assertEquals(star(350f), Pair(320f, 9))

        assertEquals(star(90f), Pair(90f, 4))
        assertEquals(star(95f), Pair(90f, 4))
    }


}
