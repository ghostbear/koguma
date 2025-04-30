package data.mediaQueryMatch

import kotlin.test.Test
import kotlin.test.expect
import me.ghostbear.koguma.data.mediaQueryParser.Scanner
import me.ghostbear.koguma.data.mediaQueryParser.Token
import me.ghostbear.koguma.data.mediaQueryParser.TokenType

class ScannerTest {

    @Test
    fun scan() {
        val source = """
            What about <<Sword Art Online>>[1], {{Re:Zero Season 2}}, or [[Eighty Six]]?
        """.trimIndent()

        val expected = listOf(
            Token(TokenType.STRING, "What", "What"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.DOUBLE_LESSER_THAN),
            Token(TokenType.STRING, "Sword", "Sword"),
            Token(TokenType.STRING, "Art", "Art"),
            Token(TokenType.STRING, "Online", "Online"),
            Token(TokenType.DOUBLE_GREATER_THAN),
            Token(TokenType.LEFT_BRACKETS),
            Token(TokenType.NUMBER, "1", 1),
            Token(TokenType.RIGHT_BRACKETS),
            Token(TokenType.STRING, ",", ","),
            Token(TokenType.LEFT_DOUBLE_BRACES),
            Token(TokenType.STRING, "Re:Zero", "Re:Zero"),
            Token(TokenType.STRING, "Season", "Season"),
            Token(TokenType.NUMBER, "2", 2),
            Token(TokenType.RIGHT_DOUBLE_BRACES),
            Token(TokenType.STRING, ",", ","),
            Token(TokenType.STRING, "or", "or"),
            Token(TokenType.LEFT_DOUBLE_BRACKETS),
            Token(TokenType.STRING, "Eighty", "Eighty"),
            Token(TokenType.STRING, "Six", "Six"),
            Token(TokenType.RIGHT_DOUBLE_BRACKETS),
            Token(TokenType.STRING, "?", "?"),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun malformedGroups() {
        val source = """
            <query>>> {{{query} [query]]]
        """.trimIndent()

        val expected = listOf(
            Token(TokenType.STRING, "<query>>>", "<query>>>"),
            Token(TokenType.STRING, "{{{query}", "{{{query}"),
            Token(TokenType.LEFT_BRACKETS),
            Token(TokenType.STRING, "query", "query"),
            Token(TokenType.STRING, "]]]", "]]]"),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun specialCharacters() {
        val source = """
            " - , ( ) / & % #
        """.trimIndent()

        val expected = listOf(
            Token(TokenType.STRING, "\"", "\""),
            Token(TokenType.STRING, "-", "-"),
            Token(TokenType.STRING, ",", ","),
            Token(TokenType.STRING, "(", "("),
            Token(TokenType.STRING, ")", ")"),
            Token(TokenType.STRING, "/", "/"),
            Token(TokenType.STRING, "&", "&"),
            Token(TokenType.STRING, "%", "%"),
            Token(TokenType.STRING, "#", "#"),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun strings() {
        val source = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

        val expected = listOf(
            Token(TokenType.STRING, "Lorem", "Lorem"),
            Token(TokenType.STRING, "ipsum", "ipsum"),
            Token(TokenType.STRING, "dolor", "dolor"),
            Token(TokenType.STRING, "sit", "sit"),
            Token(TokenType.STRING, "amet,", "amet,"),
            Token(TokenType.STRING, "consectetur", "consectetur"),
            Token(TokenType.STRING, "adipiscing", "adipiscing"),
            Token(TokenType.STRING, "elit.", "elit."),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun numbers() {
        val source = """
            8 31.1
        """.trimIndent()

        val expected = listOf(
            Token(TokenType.NUMBER, "8", 8),
            Token(TokenType.NUMBER, "31.1", 31.1),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun angleBracket() {
        val source = "<<Sword Art Online>>"

        val expected = listOf(
            Token(TokenType.DOUBLE_LESSER_THAN),
            Token(TokenType.STRING, "Sword", "Sword"),
            Token(TokenType.STRING, "Art", "Art"),
            Token(TokenType.STRING, "Online", "Online"),
            Token(TokenType.DOUBLE_GREATER_THAN),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun braces() {
        val source = "{{Re:Zero}}"

        val expected = listOf(
            Token(TokenType.LEFT_DOUBLE_BRACES),
            Token(TokenType.STRING, "Re:Zero", "Re:Zero"),
            Token(TokenType.RIGHT_DOUBLE_BRACES),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }

    @Test
    fun brackets() {
        val source = "[[Eighty Six]]"

        val expected = listOf(
            Token(TokenType.LEFT_DOUBLE_BRACKETS),
            Token(TokenType.STRING, "Eighty", "Eighty"),
            Token(TokenType.STRING, "Six", "Six"),
            Token(TokenType.RIGHT_DOUBLE_BRACKETS),
            Token(TokenType.EOF, ""),
        )
        expect(expected) { Scanner(source).scanTokens() }
    }



}