package com.lab3.scicalc.core

/**
 * Recursive-descent parser.
 *
 * Grammar (lowest to highest precedence):
 * ```
 * expr    := term (('+' | '-') term)*
 * term    := unary (('*' | '/') unary | <implicit> unary)*
 * unary   := ('-' | '+') unary | power
 * power   := postfix ('^' unary)?          // right-associative: 2^3^2 = 2^9
 * postfix := primary '!'*
 * primary := NUMBER | 'x' | 'pi' | 'e' | FUNC primary | '(' expr ')'
 * ```
 * Implicit multiplication is supported: `2x`, `3sin(x)`, `(x+1)(x-1)`.
 * Note `-x^2` means `-(x^2)`, as in standard mathematical notation.
 */
class Parser private constructor(private val tokens: List<Token>) {

    private var index = 0
    private val current: Token get() = tokens[index]

    companion object {
        fun parse(input: String): Expr {
            if (input.isBlank()) throw CalcException(CalcError.EMPTY)
            val parser = Parser(Lexer.tokenize(input))
            val expr = parser.parseExpr()
            when (val t = parser.current) {
                is Token.End -> {}
                is Token.RParen -> throw CalcException(CalcError.MISMATCHED_PAREN, ")", t.pos)
                else -> throw CalcException(CalcError.UNEXPECTED_CHAR, describe(t), t.pos)
            }
            return expr
        }

        private fun describe(t: Token): String = when (t) {
            is Token.Number -> t.value.toString()
            is Token.Ident -> t.name
            is Token.Op -> t.symbol.toString()
            is Token.LParen -> "("
            is Token.RParen -> ")"
            is Token.End -> ""
        }
    }

    private fun advance(): Token = tokens[index].also { if (index < tokens.lastIndex) index++ }

    private fun isOp(c: Char) = (current as? Token.Op)?.symbol == c

    private fun parseExpr(): Expr {
        var left = parseTerm()
        while (true) {
            left = when {
                isOp('+') -> { advance(); Expr.Add(left, parseTerm()) }
                isOp('-') -> { advance(); Expr.Sub(left, parseTerm()) }
                else -> return left
            }
        }
    }

    private fun parseTerm(): Expr {
        var left = parseUnary()
        while (true) {
            left = when {
                isOp('*') -> { advance(); Expr.Mul(left, parseUnary()) }
                isOp('/') -> { advance(); Expr.Div(left, parseUnary()) }
                startsPrimary(current) -> Expr.Mul(left, parsePower())
                else -> return left
            }
        }
    }

    private fun startsPrimary(t: Token) = t is Token.Number || t is Token.Ident || t is Token.LParen

    private fun parseUnary(): Expr = when {
        isOp('-') -> { advance(); Expr.Neg(parseUnary()) }
        isOp('+') -> { advance(); parseUnary() }
        else -> parsePower()
    }

    private fun parsePower(): Expr {
        val base = parsePostfix()
        if (isOp('^')) {
            advance()
            return Expr.Pow(base, parseUnary())
        }
        return base
    }

    private fun parsePostfix(): Expr {
        var e = parsePrimary()
        while (isOp('!')) {
            advance()
            e = Expr.Factorial(e)
        }
        return e
    }

    private fun parsePrimary(): Expr {
        val t = advance()
        return when (t) {
            is Token.Number -> Expr.Num(t.value)
            is Token.LParen -> {
                val inner = parseExpr()
                if (current !is Token.RParen) {
                    throw CalcException(CalcError.MISMATCHED_PAREN, "(", t.pos)
                }
                advance()
                inner
            }
            is Token.Ident -> when (t.name) {
                "x" -> Expr.X
                "pi" -> Expr.PI
                "e" -> Expr.E
                else -> {
                    val fn = Fn.find(t.name)
                        ?: throw CalcException(CalcError.UNKNOWN_FUNCTION, t.name, t.pos)
                    // function argument: "sin(x)", or without parentheses "√2", "sin x"
                    if (current is Token.End) throw CalcException(CalcError.UNEXPECTED_END)
                    Expr.Call(fn, parsePostfix())
                }
            }
            is Token.End -> throw CalcException(CalcError.UNEXPECTED_END)
            is Token.Op -> throw CalcException(CalcError.UNEXPECTED_CHAR, t.symbol.toString(), t.pos)
            is Token.RParen -> throw CalcException(CalcError.MISMATCHED_PAREN, ")", t.pos)
        }
    }
}
