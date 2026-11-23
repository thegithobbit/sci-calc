package com.lab3.scicalc.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Evaluates an [Expr] to a number.
 *
 * @param x value of the variable `x`, or null if `x` is not allowed (plain calculator mode)
 * @param angleMode how trigonometric functions interpret angles
 */
class Evaluator(
    private val x: Double? = null,
    private val angleMode: AngleMode = AngleMode.RAD,
) {

    fun evaluate(e: Expr): Double {
        val v = eval(e)
        if (v.isNaN()) throw CalcException(CalcError.DOMAIN)
        if (v.isInfinite()) throw CalcException(CalcError.OVERFLOW)
        return v
    }

    private fun eval(e: Expr): Double = when (e) {
        is Expr.Num -> e.value
        is Expr.Const -> e.value
        Expr.X -> x ?: throw CalcException(CalcError.VARIABLE_NOT_ALLOWED, "x")
        is Expr.Neg -> -eval(e.arg)
        is Expr.Add -> eval(e.left) + eval(e.right)
        is Expr.Sub -> eval(e.left) - eval(e.right)
        is Expr.Mul -> eval(e.left) * eval(e.right)
        is Expr.Div -> {
            val d = eval(e.right)
            if (d == 0.0) throw CalcException(CalcError.DIVISION_BY_ZERO)
            eval(e.left) / d
        }
        is Expr.Pow -> power(eval(e.base), eval(e.exponent))
        is Expr.Factorial -> factorial(eval(e.arg))
        is Expr.Call -> call(e.fn, eval(e.arg))
    }

    private fun power(b: Double, p: Double): Double {
        if (b == 0.0 && p < 0) throw CalcException(CalcError.DIVISION_BY_ZERO)
        // Real odd roots of negative numbers: (-8)^(1/3) = -2
        if (b < 0 && p != floor(p)) {
            val inv = 1.0 / p
            val n = Math.round(inv)
            if (abs(inv - n) < 1e-9 && n % 2L != 0L) return -((-b).pow(p))
            throw CalcException(CalcError.DOMAIN)
        }
        return b.pow(p)
    }

    private fun factorial(v: Double): Double {
        if (v < 0 || v != floor(v)) throw CalcException(CalcError.DOMAIN, "!")
        if (v > 170) throw CalcException(CalcError.OVERFLOW)
        var r = 1.0
        for (i in 2..v.toInt()) r *= i
        return r
    }

    private fun call(fn: Fn, a: Double): Double {
        val arg = if (fn.takesAngle && angleMode == AngleMode.DEG) Math.toRadians(a) else a
        val result = when (fn) {
            Fn.SIN -> if (isDegMultiple(a, 180.0)) 0.0 else sin(arg)
            Fn.COS -> if (isDegMultiple(a - 90.0, 180.0)) 0.0 else cos(arg)
            Fn.TAN -> {
                if (isDegMultiple(a - 90.0, 180.0)) throw CalcException(CalcError.DOMAIN, fn.id)
                if (isDegMultiple(a, 180.0)) 0.0 else tan(arg)
            }
            Fn.COT -> {
                if (isDegMultiple(a, 180.0)) throw CalcException(CalcError.DOMAIN, fn.id)
                val t = tan(arg)
                if (t == 0.0) throw CalcException(CalcError.DOMAIN, fn.id)
                if (isDegMultiple(a - 90.0, 180.0)) 0.0 else 1.0 / t
            }
            Fn.ASIN -> { requireDomain(a in -1.0..1.0, fn); asin(a) }
            Fn.ACOS -> { requireDomain(a in -1.0..1.0, fn); acos(a) }
            Fn.ATAN -> atan(a)
            Fn.SINH -> sinh(a)
            Fn.COSH -> cosh(a)
            Fn.TANH -> tanh(a)
            Fn.LN -> { requireDomain(a > 0, fn); ln(a) }
            Fn.LOG -> { requireDomain(a > 0, fn); log10(a) }
            Fn.SQRT -> { requireDomain(a >= 0, fn); sqrt(a) }
            Fn.EXP -> exp(a)
            Fn.ABS -> abs(a)
        }
        return if (fn.returnsAngle && angleMode == AngleMode.DEG) Math.toDegrees(result) else result
    }

    /** In DEG mode returns exact zeros for sin(180°k), cos(90°+180°k) instead of 1e-16 noise. */
    private fun isDegMultiple(a: Double, step: Double): Boolean =
        angleMode == AngleMode.DEG && a % step == 0.0

    private fun requireDomain(ok: Boolean, fn: Fn) {
        if (!ok) throw CalcException(CalcError.DOMAIN, fn.id)
    }
}
