package com.lab3.scicalc.core

import com.lab3.scicalc.core.Expr.Add
import com.lab3.scicalc.core.Expr.Call
import com.lab3.scicalc.core.Expr.Div
import com.lab3.scicalc.core.Expr.Mul
import com.lab3.scicalc.core.Expr.Neg
import com.lab3.scicalc.core.Expr.Num
import com.lab3.scicalc.core.Expr.Pow
import com.lab3.scicalc.core.Expr.Sub
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Algebraic simplifier. Applies rewrite rules bottom-up until a fixed point:
 * constant folding, neutral/absorbing elements (x+0, x·1, x·0, x^1, x^0),
 * sign normalisation, collecting like terms (2x + 3x = 5x),
 * merging powers (x·x^2 = x^3, x^5/x^2 = x^3) and moving coefficients to the front.
 *
 * It is not a full CAS, but it keeps derivatives readable:
 * d/dx x^3 gives `3x^2` instead of `3*x^(3-1)*1`.
 */
object Simplifier {

    private const val MAX_PASSES = 50

    fun simplify(e: Expr): Expr {
        var current = e
        repeat(MAX_PASSES) {
            val next = step(current)
            if (next == current) return current
            current = next
        }
        return current
    }

    private fun step(e: Expr): Expr = when (e) {
        is Num, is Expr.Const, Expr.X -> e
        is Neg -> neg(step(e.arg))
        is Add -> add(step(e.left), step(e.right))
        is Sub -> sub(step(e.left), step(e.right))
        is Mul -> mul(step(e.left), step(e.right))
        is Div -> div(step(e.left), step(e.right))
        is Pow -> pow(step(e.base), step(e.exponent))
        is Call -> call(e.fn, step(e.arg))
        is Expr.Factorial -> Expr.Factorial(step(e.arg))
    }

    // ---------- helpers ----------

    private fun Expr.num(): Double? = (this as? Num)?.value
    private fun Expr.isNum(v: Double) = this is Num && value == v
    private fun isInt(v: Double) = v == floor(v) && abs(v) < 1e15

    /** Splits `c·rest` into (c, rest). For an expression without a numeric factor c = 1. */
    private fun splitCoeff(e: Expr): Pair<Double, Expr> = when {
        e is Mul && e.left is Num -> e.left.value to e.right
        e is Neg -> splitCoeff(e.arg).let { (c, r) -> -c to r }
        else -> 1.0 to e
    }

    private fun withCoeff(c: Double, rest: Expr): Expr = when (c) {
        0.0 -> Expr.ZERO
        1.0 -> rest
        -1.0 -> Neg(rest)
        else -> Mul(Num(c), rest)
    }

    /** `p + c` -> (p, c), `p - c` -> (p, -c) for a numeric c; otherwise null. */
    private fun splitConst(e: Expr): Pair<Expr, Double>? = when {
        e is Add && e.right is Num -> e.left to e.right.value
        e is Sub && e.right is Num -> e.left to -e.right.value
        else -> null
    }

    /** Splits `b^n` into (b, n); a plain expression is `e^1`. */
    private fun splitPow(e: Expr): Pair<Expr, Expr> =
        if (e is Pow) e.base to e.exponent else e to Expr.ONE

    // ---------- rules ----------

    private fun neg(a: Expr): Expr = when {
        a is Num -> Num(-a.value)
        a is Neg -> a.arg
        a is Mul && a.left is Num -> withCoeff(-a.left.value, a.right)
        else -> Neg(a)
    }

    private fun add(a: Expr, b: Expr): Expr {
        val av = a.num()
        val bv = b.num()
        if (av != null && bv != null) return Num(av + bv)
        if (a.isNum(0.0)) return b
        if (b.isNum(0.0)) return a
        if (bv != null && bv < 0) return Sub(a, Num(-bv))
        if (b is Neg) return Sub(a, b.arg)
        if (a is Neg) return Sub(b, a.arg)
        if (av != null) return Add(b, a) // constants go last: x + 1
        if (bv != null) splitConst(a)?.let { (rest, c) -> return add(rest, Num(c + bv)) } // (p + 1) + 2 = p + 3

        val (ca, ra) = splitCoeff(a)
        val (cb, rb) = splitCoeff(b)
        if (ra == rb) return withCoeff(ca + cb, ra)          // 2x + 3x = 5x
        if (cb < 0) return Sub(a, withCoeff(-cb, rb))         // a + (-3x) = a - 3x
        return Add(a, b)
    }

    private fun sub(a: Expr, b: Expr): Expr {
        val av = a.num()
        val bv = b.num()
        if (av != null && bv != null) return Num(av - bv)
        if (b.isNum(0.0)) return a
        if (a.isNum(0.0)) return neg(b)
        if (a == b) return Expr.ZERO
        if (bv != null && bv < 0) return Add(a, Num(-bv))
        if (b is Neg) return Add(a, b.arg)
        if (bv != null) splitConst(a)?.let { (rest, c) -> return add(rest, Num(c - bv)) } // (p + 1) - 1 = p

        val (ca, ra) = splitCoeff(a)
        val (cb, rb) = splitCoeff(b)
        if (ra == rb) return withCoeff(ca - cb, ra)          // 5x - 2x = 3x
        if (cb < 0) return Add(a, withCoeff(-cb, rb))         // a - (-3x) = a + 3x
        return Sub(a, b)
    }

    private fun mul(a: Expr, b: Expr): Expr {
        val av = a.num()
        val bv = b.num()
        if (av != null && bv != null) return Num(av * bv)
        if (a.isNum(0.0) || b.isNum(0.0)) return Expr.ZERO
        if (a.isNum(1.0)) return b
        if (b.isNum(1.0)) return a
        if (a.isNum(-1.0)) return neg(b)
        if (b.isNum(-1.0)) return neg(a)
        if (a is Neg) return neg(Mul(a.arg, b))
        if (b is Neg) return neg(Mul(a, b.arg))
        if (bv != null) return Mul(b, a) // coefficient first: x·3 = 3x

        // collect numeric coefficients at the front
        if (av != null && b is Mul && b.left is Num) return Mul(Num(av * b.left.value), b.right)
        if (av == null && b is Mul && b.left is Num) return Mul(b.left, Mul(a, b.right))
        if (av == null && a is Mul && a.left is Num) return Mul(a.left, Mul(a.right, b))

        // pull division out: a·(n/d) = (a·n)/d
        if (b is Div) return Div(Mul(a, b.left), b.right)
        if (a is Div) return Div(Mul(a.left, b), a.right)

        // same base: x^m · x^n = x^(m+n)
        val (ba, ea) = splitPow(a)
        val (bb, eb) = splitPow(b)
        if (ba == bb && ba !is Num) return Pow(ba, Add(ea, eb))

        // x · (x · c) = x^2 · c   and   (c · x) · x = c · x^2
        if (b is Mul && ba !is Num && splitPow(b.left).first == ba) return Mul(Mul(a, b.left), b.right)
        if (a is Mul && bb !is Num && splitPow(a.right).first == bb) return Mul(a.left, Mul(a.right, b))

        // powers of x go first: cos(x)·x = x·cos(x)
        if (av == null && isMonomial(b) && !isMonomial(a)) return Mul(b, a)

        return Mul(a, b)
    }

    private fun isMonomial(e: Expr) = e == Expr.X || (e is Pow && e.base == Expr.X && e.exponent is Num)

    private fun div(a: Expr, b: Expr): Expr {
        val av = a.num()
        val bv = b.num()
        if (bv == 0.0) return Div(a, b) // left for the evaluator to report
        if (av != null && bv != null) return foldFraction(av, bv)
        if (a.isNum(0.0)) return Expr.ZERO
        if (b.isNum(1.0)) return a
        if (b.isNum(-1.0)) return neg(a)
        if (a == b) return Expr.ONE
        if (a is Neg) return neg(Div(a.arg, b))
        if (b is Neg) return neg(Div(a, b.arg))
        if (a is Div) return Div(a.left, Mul(a.right, b))   // (p/q)/r = p/(q·r)
        if (b is Div) return Div(Mul(a, b.right), b.left)   // p/(q/r) = p·r/q

        // sign goes to the front: (-2x)/y = -(2x/y)
        val (ca, ra) = splitCoeff(a)
        val (cb, rb) = splitCoeff(b)
        if (ca < 0) return neg(Div(withCoeff(-ca, ra), b))

        // (6x)/2 = 3x,  (2x)/(2y) = x/y,  (4x)/(6y) = (2x)/(3y)
        if (cb != 1.0 && isInt(ca) && isInt(cb)) {
            val g = gcd(abs(ca).toLong(), abs(cb).toLong()).toDouble()
            if (g > 1.0) return Div(withCoeff(ca / g, ra), withCoeff(cb / g, rb))
        }
        // (u·v)/v = u
        if (a is Mul && a.right == b) return a.left
        if (a is Mul && a.left == b) return a.right

        // same base: x^m / x^n = x^(m-n)
        val (ba, ea) = splitPow(a)
        val (bb, eb) = splitPow(b)
        if (ba == bb && ba !is Num) return Pow(ba, Sub(ea, eb))

        // cancel a factor of the numerator: (u·cos(x)) / cos(x)^4 = u / cos(x)^3
        if (bb !is Num) {
            if (splitPow(ra).first == bb) return withCoeff(ca, Div(ra, b))
            if (ra is Mul && splitPow(ra.left).first == bb) return withCoeff(ca, Mul(Div(ra.left, b), ra.right))
            if (ra is Mul && splitPow(ra.right).first == bb) return withCoeff(ca, Mul(ra.left, Div(ra.right, b)))
        }

        return Div(a, b)
    }

    /** Folds a/b only when exact; otherwise reduces the fraction (6/4 -> 3/2). */
    private fun foldFraction(a: Double, b: Double): Expr {
        val q = a / b
        if (isInt(q)) return Num(q)
        if (isInt(a) && isInt(b)) {
            val g = gcd(abs(a).toLong(), abs(b).toLong()).toDouble()
            val sign = if (b < 0) -1 else 1
            val na = sign * a / g
            val nb = sign * b / g
            return if (na < 0) Neg(Div(Num(-na), Num(nb))) else Div(Num(na), Num(nb))
        }
        return Num(q)
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

    private fun pow(b: Expr, ex: Expr): Expr {
        val bv = b.num()
        val ev = ex.num()
        if (ev == 0.0) return Expr.ONE
        if (ev == 1.0) return b
        if (bv == 1.0) return Expr.ONE
        if (bv == 0.0 && ev != null && ev > 0) return Expr.ZERO
        if (bv != null && ev != null && isInt(ev) && ev > 0) {
            val r = bv.pow(ev)
            if (isInt(r)) return Num(r)
        }
        // (x^m)^n = x^(m·n) for numeric m, n
        if (b is Pow && b.exponent is Num && ev != null) {
            return Pow(b.base, Num(b.exponent.value * ev))
        }
        // (√u)^2 = u
        if (b is Call && b.fn == Fn.SQRT && ev == 2.0) return b.arg
        // x^(-n) = 1/x^n
        if (ev != null && ev < 0) return Div(Expr.ONE, Pow(b, Num(-ev)))
        return Pow(b, ex)
    }

    private fun call(fn: Fn, a: Expr): Expr {
        if (a.isNum(0.0)) {
            when (fn) {
                Fn.SIN, Fn.TAN, Fn.ASIN, Fn.ATAN, Fn.SINH, Fn.TANH, Fn.SQRT, Fn.ABS -> return Expr.ZERO
                Fn.COS, Fn.COSH, Fn.EXP -> return Expr.ONE
                else -> {}
            }
        }
        if (fn == Fn.LN && a == Expr.E) return Expr.ONE
        if ((fn == Fn.LN || fn == Fn.LOG) && a.isNum(1.0)) return Expr.ZERO
        if (fn == Fn.LOG && a.isNum(10.0)) return Expr.ONE
        if (fn == Fn.LN && a is Call && a.fn == Fn.EXP) return a.arg
        return Call(fn, a)
    }
}
