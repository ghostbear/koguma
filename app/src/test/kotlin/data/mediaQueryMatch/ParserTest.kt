package data.mediaQueryMatch

import kotlin.test.Test
import kotlin.test.expect
import me.ghostbear.koguma.data.mediaQueryParser.Expr
import me.ghostbear.koguma.data.mediaQueryParser.Parser
import me.ghostbear.koguma.data.mediaQueryParser.Scanner
import me.ghostbear.koguma.data.mediaQueryParser.Token
import me.ghostbear.koguma.data.mediaQueryParser.TokenType
import org.junit.jupiter.api.Disabled

class ParserTest {
    @Test
    fun expression() {
        val tokens = listOf(
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

        val expected = listOf(
            Expr.Binary(
                Expr.Group(
                    TokenType.DOUBLE_LESSER_THAN,
                    listOf(
                        Expr.Literal("Sword"),
                        Expr.Literal("Art"),
                        Expr.Literal("Online"),
                    ),
                    TokenType.DOUBLE_GREATER_THAN
                ),
                Expr.Index(Expr.Literal(1))
            ),
            Expr.Group(
                TokenType.LEFT_DOUBLE_BRACES,
                listOf(
                    Expr.Literal("Re:Zero"),
                ),
                TokenType.RIGHT_DOUBLE_BRACES
            ),
            Expr.Group(
                TokenType.LEFT_DOUBLE_BRACKETS,
                listOf(
                    Expr.Literal("Eighty"),
                    Expr.Literal("Six"),
                ),
                TokenType.RIGHT_DOUBLE_BRACKETS
            )
        )
        expect(expected) { Parser(tokens).expression() }
    }

    @Test
    fun expressionMalformedInput() {
        val tokens = listOf(
            Token(TokenType.STRING, "What", "What"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.STRING, "<Sword", "<Sword"),
            Token(TokenType.STRING, "Art", "Art"),
            Token(TokenType.STRING, "Online", "Online"),
            Token(TokenType.STRING, ">>>,", ">>>,"),
            Token(TokenType.STRING, "{{{Re:Zero},", "{{{Re:Zero},"),
            Token(TokenType.STRING, "or", "or"),
            Token(TokenType.LEFT_BRACKETS),
            Token(TokenType.STRING, "Eighty", "Eighty"),
            Token(TokenType.STRING, "Six", "Six"),
            Token(TokenType.STRING, "]]]?", "]]]?"),
            Token(TokenType.EOF, ""),
        )
        expect(emptyList()) { Parser(tokens).expression() }
    }

    @Test
    fun expressionThirtyFirst() {
        val tokens = listOf(
            Token(TokenType.STRING, "How", "How"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.STRING, "we", "we"),
            Token(TokenType.STRING, "read", "read"),
            Token(TokenType.DOUBLE_LESSER_THAN),
            Token(TokenType.STRING, "Long", "Long"),
            Token(TokenType.STRING, "Summer", "Summer"),
            Token(TokenType.STRING, "of", "of"),
            Token(TokenType.STRING, "August", "August"),
            Token(TokenType.NUMBER, "31", 31),
            Token(TokenType.STRING, "st", "st"),
            Token(TokenType.DOUBLE_GREATER_THAN),
            Token(TokenType.STRING, "instead?", "instead?"),
            Token(TokenType.EOF, ""),
        )
        val expected = listOf(
            Expr.Group(
                TokenType.DOUBLE_LESSER_THAN,
                listOf(
                    Expr.Literal("Long"),
                    Expr.Literal("Summer"),
                    Expr.Literal("of"),
                    Expr.Literal("August"),
                    Expr.Literal(31),
                    Expr.Literal("st"),
                ),
                TokenType.DOUBLE_GREATER_THAN
            )
        )
        expect(expected) { Parser(tokens).expression() }
    }

    @Test
    @Disabled
    fun expressionSwappedTokens() {
        val tokens = listOf(
            Token(TokenType.STRING, "What", "What"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.DOUBLE_GREATER_THAN),
            Token(TokenType.STRING, "Sword", "Sword"),
            Token(TokenType.STRING, "Art", "Art"),
            Token(TokenType.STRING, "Online", "Online"),
            Token(TokenType.DOUBLE_LESSER_THAN),
            Token(TokenType.LEFT_BRACKETS),
            Token(TokenType.NUMBER, "1", 1),
            Token(TokenType.RIGHT_BRACKETS),
            Token(TokenType.EOF, ""),
        )

        expect(emptyList()) { Parser(tokens).expression() }
    }

    @Test
    fun expressionSpecialCharacters() {
        val tokens = listOf(
            Token(TokenType.STRING, "How", "How"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.STRING, "we", "we"),
            Token(TokenType.STRING, "read", "read"),
            Token(TokenType.DOUBLE_LESSER_THAN),
            Token(TokenType.NUMBER, "8", 8),
            Token(TokenType.STRING, "-gatsu", "-gatsu"),
            Token(TokenType.NUMBER, "31", 31),
            Token(TokenType.STRING, "-nichi", "-nichi"),
            Token(TokenType.STRING, "no", "no"),
            Token(TokenType.STRING, "Long", "Long"),
            Token(TokenType.STRING, "Summer", "Summer"),
            Token(TokenType.DOUBLE_GREATER_THAN),
            Token(TokenType.STRING, "instead?", "instead?"),
            Token(TokenType.EOF, ""),
        )
        val expected = listOf(
            Expr.Group(
                TokenType.DOUBLE_LESSER_THAN,
                listOf(
                    Expr.Literal(8),
                    Expr.Literal("-gatsu"),
                    Expr.Literal(31),
                    Expr.Literal("-nichi"),
                    Expr.Literal("no"),
                    Expr.Literal(        "Long"),
                    Expr.Literal("Summer"),
                ),
                TokenType.DOUBLE_GREATER_THAN
            )
        )
        expect(expected) { Parser(tokens).expression() }
    }

    @Test
    fun expressionJustText() {
        val tokens = listOf(
            Token(TokenType.STRING, "What", "What"),
            Token(TokenType.STRING, "about", "about"),
            Token(TokenType.STRING, "Sword", "Sword"),
            Token(TokenType.STRING, "Art", "Art"),
            Token(TokenType.STRING, "Online", "Online"),
            Token(TokenType.STRING, "?", "?"),
            Token(TokenType.EOF, ""),
        )
        expect(emptyList()) { Parser(tokens).expression() }
    }

}