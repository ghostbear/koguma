package data.mediaQueryMatch

import kotlin.test.expect
import me.ghostbear.koguma.data.mediaQueryParser.Expr
import me.ghostbear.koguma.data.mediaQueryParser.InterpreterMediaQueryMatch
import me.ghostbear.koguma.data.mediaQueryParser.InterpreterMediaQueryMatcher
import me.ghostbear.koguma.data.mediaQueryParser.TokenType
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InterpreterMediaQueryMatcherTest {
    @Test
    fun match() {
        val expected = MediaQueryResults(
            listOf(
                InterpreterMediaQueryMatch(
                    Expr.Group(
                        TokenType.DOUBLE_LESSER_THAN,
                        listOf(
                            Expr.Literal("Sword"),
                            Expr.Literal("Art"),
                            Expr.Literal("Online"),
                        ),
                        TokenType.DOUBLE_GREATER_THAN
                    )
                )
            )
        )

        val actual = InterpreterMediaQueryMatcher().match("<<Sword Art Online>>")
        assertEquals(expected, actual)

        val match = actual.matches.first()
        assertEquals("Sword Art Online", match.query)
    }

    @Test
    fun specialCharactersAndNumbers() {
        val actual = InterpreterMediaQueryMatcher().match("<<8-gatsu 31-nichi no Long Summer>>")
        assertEquals(1, actual.matches.size)

        val match = actual.matches.first()
        assertEquals("8 -gatsu 31 -nichi no Long Summer", match.query)
    }

}