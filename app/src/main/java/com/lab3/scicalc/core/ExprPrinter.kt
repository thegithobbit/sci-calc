package com.lab3.scicalc.core

/**
 * Converts an [Expr] back to text with the minimum number of parentheses.
 * The output can be parsed again by [Parser], so a derivative can be copied
 * back into the input field.
 */
object ExprPrinter {

    private const val P_ADD = 1
    private const val P_MUL = 2
    private const val P_NEG = 3
    private const val P_POW = 4
    private const val P_POSTFIX = 5
    private const val P_ATOM = 6

    fun print(e: Expr): String = p(e)

    private fun prec(e: Expr): Int = when (e) {
        is Expr.Add, is Expr.Sub -> P_ADD
        is Expr.Mul, is Expr.Div -> P_MUL
        is Expr.Neg -> P_NEG
        is Expr.Num -> if (e.value < 0) P_NEG else P_ATOM
        is Expr.Pow -> P_POW
        is Expr.Factorial -> P_POSTFIX
        else -> P_ATOM
    }

    private fun isNegative(e: Expr) = prec(e) == P_NEG

    private fun wrap(e: Expr, parens: Boolean) = if (parens) "(${p(e)})" else p(e)

    private fun p(e: Expr): String = when (e) {
        is Expr.Num -> NumberFormatter.format(e.value)
        Expr.X -> "x"
        is Expr.Const -> e.symbol
        is Expr.Neg -> p(e.arg).let { inner ->
            if (prec(e.arg) == P_ADD || inner.startsWith("-")) "-($inner)" else "-$inner"
        }
        is Expr.Add -> p(e.left) + " + " + wrap(e.right, isNegative(e.right))
        is Expr.Sub -> p(e.left) + " - " + wrap(e.right, prec(e.right) == P_ADD || isNegative(e.right))
        is Expr.Mul -> printMul(e)
        is Expr.Div -> wrap(e.left, prec(e.left) == P_ADD) + "/" + wrap(e.right, prec(e.right) <= P_NEG)
        is Expr.Pow -> wrap(e.base, prec(e.base) <= P_POSTFIX) + "^" + wrap(e.exponent, prec(e.exponent) < P_ATOM)
        is Expr.Factorial -> wrap(e.arg, prec(e.arg) < P_ATOM) + "!"
        is Expr.Call -> if (e.fn == Fn.SQRT) "√(${p(e.arg)})" else "${e.fn.id}(${p(e.arg)})"
    }

    /** `2x`, `3sin(x)`, `2(x + 1)` use implicit multiplication; everything else uses `·`. */
    private fun printMul(e: Expr.Mul): String {
        val left = wrap(e.left, prec(e.left) == P_ADD)
        val right = wrap(e.right, prec(e.right) == P_ADD || isNegative(e.right))
        val implicit = e.left is Expr.Num && (right.first().isLetter() || right.first() == '(' ||
            right.first() == '√' || right.first() == 'π')
        return if (implicit) left + right else "$left·$right"
    }
}
