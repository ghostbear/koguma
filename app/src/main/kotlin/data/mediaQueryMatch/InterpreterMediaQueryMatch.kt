package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatch

class InterpreterMediaQueryMatch(private val expr: Expr) : MediaQueryMatch {
    override val query: String
        get() = query(expr) ?: throw IllegalStateException("Unparsable query")

    fun query(expr: Expr): String? {
        return when (expr) {
            is Expr.Group -> expr.literals.joinToString("") { it?.literal?.toString() ?: "" }
            is Expr.Binary -> query(expr.left) ?: query(expr.right)
            else -> throw IllegalStateException("Unexpected expression: $expr")
        }
    }

    override val type: MediaType
        get() = type(expr) ?: throw IllegalStateException("Unparsable type")

    fun type(expr: Expr): MediaType? {
        return when (expr) {
            is Expr.Group -> when (expr.leftType) {
                TokenType.LEFT_DOUBLE_BRACES -> MediaType.ANIME
                TokenType.LEFT_DOUBLE_BRACKETS -> MediaType.NOVEL
                TokenType.DOUBLE_LESSER_THAN -> MediaType.MANGA
                else -> throw IllegalStateException("Unexpected type: ${expr.leftType}")
            }
            is Expr.Binary -> type(expr.left) ?: type(expr.right)
            else -> throw IllegalStateException("Unexpected expression: $expr")
        }
    }

    override val page: Int
        get() = page(expr) ?: 1

    fun page(expr: Expr): Int? {
        return when (expr) {
            is Expr.Group -> null
            is Expr.Index -> (expr.literal?.literal as Number).toInt()
            is Expr.Binary -> page(expr.left) ?: page(expr.right)
            else -> throw IllegalStateException("Unexpected expression: $expr")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterpreterMediaQueryMatch

        return expr == other.expr
    }

    override fun hashCode(): Int {
        return expr.hashCode()
    }

}