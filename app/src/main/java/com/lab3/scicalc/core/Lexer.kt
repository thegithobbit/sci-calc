package com.lab3.scicalc.core

sealed class Token(val pos: Int) {
    class Number(val value: Double, pos: Int) : Token(pos)
    class Ident(val name: String, pos: Int) : Token(pos)
    class Op(val symbol: Char, pos: Int) : Token(pos)
    class LParen(pos: Int) : Token(pos)
    class RParen(pos: Int) : Token(pos)
    class End(pos: Int) : Token(pos)
}

/**
 * Splits input into tokens. Accepts both ASCII operators and the
 * "pretty" symbols used on the calculator keypad (×, ÷, −, √, π).
 */
object Lexer {

    fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || (c == '.' && i + 1 < input.length && input[i + 1].isDigit()) -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    // scientific notation uses capital E only: 1.5E3, 2E-4.
                    // Lowercase e is always Euler's number, so "2e3" means 2·e·3.
                    if (i < input.length && input[i] == 'E') {
                        var j = i + 1
                        if (j < input.length && (input[j] == '+' || input[j] == '-')) j++
                        if (j < input.length && input[j].isDigit()) {
                            i = j
                            while (i < input.length && input[i].isDigit()) i++
                        }
                    }
                    val text = input.substring(start, i)
                    val value = text.toDoubleOrNull()
                        ?: throw CalcException(CalcError.UNEXPECTED_CHAR, text, start)
                    tokens += Token.Number(value, start)
                }

                c.isLetter() && c != 'π' -> {
                    val start = i
                    while (i < input.length && input[i].isLetter() && input[i] != 'π') i++
                    tokens += splitIdentifier(input.substring(start, i), start)
                }

                c == 'π' -> { tokens += Token.Ident("pi", i); i++ }
                c == '√' -> { tokens += Token.Ident("sqrt", i); i++ }
                c == '(' -> { tokens += Token.LParen(i); i++ }
                c == ')' -> { tokens += Token.RParen(i); i++ }
                c == '+' -> { tokens += Token.Op('+', i); i++ }
                c == '-' || c == '−' -> { tokens += Token.Op('-', i); i++ }
                c == '*' || c == '×' || c == '·' -> { tokens += Token.Op('*', i); i++ }
                c == '/' || c == '÷' -> { tokens += Token.Op('/', i); i++ }
                c == '^' -> { tokens += Token.Op('^', i); i++ }
                c == '!' -> { tokens += Token.Op('!', i); i++ }
                c == '²' -> { tokens += Token.Op('^', i); tokens += Token.Number(2.0, i); i++ }
                c == '³' -> { tokens += Token.Op('^', i); tokens += Token.Number(3.0, i); i++ }
                else -> throw CalcException(CalcError.UNEXPECTED_CHAR, c.toString(), i)
            }
        }
        tokens += Token.End(input.length)
        return tokens
    }

    private val knownWords: List<String> =
        (Fn.entries.map { it.id } + listOf("pi", "x", "e")).sortedByDescending { it.length }

    /**
     * Words like "xsin" or "ex" are split greedily into known names ("x","sin" / "e","x"),
     * which makes implicit multiplication like `2xsin(x)` work. Unknown words are errors.
     */
    private fun splitIdentifier(word: String, start: Int): List<Token> {
        val lower = word.lowercase()
        val result = mutableListOf<Token>()
        var k = 0
        while (k < lower.length) {
            val match = knownWords.firstOrNull { lower.startsWith(it, k) }
                ?: throw CalcException(CalcError.UNKNOWN_FUNCTION, word, start)
            result += Token.Ident(match, start + k)
            k += match.length
        }
        return result
    }
}
