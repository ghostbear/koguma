package me.ghostbear.koguma.data.mediaQueryMatch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InterpreterMediaQueryMatcherTest {
    @Test
    fun match() {
        val actual = InterpreterMediaQueryMatcher().match("<<Sword Art Online>>")
        assertEquals(1, actual.matches.size)

        val match = actual.matches.first()
        assertEquals("Sword Art Online", match.query)
    }

    @Test
    fun specialCharactersAndNumbers() {
        val actual = InterpreterMediaQueryMatcher().match("<<8-gatsu 31-nichi no Long Summer>>")
        assertEquals(1, actual.matches.size)

        val match = actual.matches.first()
        assertEquals("8-gatsu 31-nichi no Long Summer", match.query)
    }

}