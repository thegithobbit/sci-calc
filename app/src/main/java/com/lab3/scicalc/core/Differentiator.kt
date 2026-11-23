package com.lab3.scicalc.core

import com.lab3.scicalc.core.Expr.Add
import com.lab3.scicalc.core.Expr.Call
import com.lab3.scicalc.core.Expr.Div
import com.lab3.scicalc.core.Expr.Mul
import com.lab3.scicalc.core.Expr.Neg
import com.lab3.scicalc.core.Expr.Num
import com.lab3.scicalc.core.Expr.Pow
import com.lab3.scicalc.core.Expr.Sub

/**
 * Symbolic differentiation with respect to `x`.
 *
 * Implements the standard rules: linearity, product rule, quotient rule,
 * power rule, the general rule for u(x)^v(x), and the chain rule
 * for every elementary function from [Fn].
 * Trigonometric derivatives assume radians.
 */
object Differentiator {

    /** n-th derivative, simplified after every step so the tree stays small. */
    fun derivative(e: Expr, order: Int = 1): Expr {
        require(order >= 1) { "order must be >= 1" }
        var result = Simplifier.simplify(e)
        repeat(order) { result = Simplifier.simplify(d(result)) }
        return result
    }

    /** Raw (unsimplified) first derivative. */
    fun d(e: Expr): Expr = when (e) {
        is Num, is Expr.Const -> Expr.ZERO
        Expr.X -> Expr.ONE
        is Neg -> Neg(d(e.arg))
        is Add -> Add(d(e.left), d(e.right))
        is Sub -> Sub(d(e.left), d(e.right))

        // (uv)' = u'v + uv'
        is Mul -> Add(Mul(d(e.left), e.right), Mul(e.left, d(e.right)))

        // (u/v)' = (u'v - uv') / v^2
        is Div -> Div(
            Sub(Mul(d(e.left), e.right), Mul(e.left, d(e.right))),
            Pow(e.right, Expr.TWO),
        )

        is Pow -> power(e.base, e.exponent)
        is Call -> Mul(outer(e.fn, e.arg), d(e.arg)) // chain rule: f(u)' = f'(u)·u'
        is Expr.Factorial ->
            if (e.arg.hasX()) throw CalcException(CalcError.NOT_DIFFERENTIABLE, "!") else Expr.ZERO
    }

    private fun power(u: Expr, v: Expr): Expr = when {
        // constant ^ constant
        !u.hasX() && !v.hasX() -> Expr.ZERO

        // (u^n)' = n·u^(n-1)·u'
        !v.hasX() -> Mul(Mul(v, Pow(u, Sub(v, Expr.ONE))), d(u))

        // (a^v)' = a^v·ln(a)·v'
        !u.hasX() -> Mul(Mul(Pow(u, v), lnOf(u)), d(v))

        // (u^v)' = u^v·(v'·ln(u) + v·u'/u)
        else -> Mul(
            Pow(u, v),
            Add(Mul(d(v), Call(Fn.LN, u)), Div(Mul(v, d(u)), u)),
        )
    }

    /** ln of a constant; ln(e) is simplified to 1 right away. */
    private fun lnOf(a: Expr): Expr = if (a == Expr.E) Expr.ONE else Call(Fn.LN, a)

    /** Derivative of the outer function f evaluated at u, i.e. f'(u). */
    private fun outer(fn: Fn, u: Expr): Expr = when (fn) {
        Fn.SIN -> Call(Fn.COS, u)
        Fn.COS -> Neg(Call(Fn.SIN, u))
        Fn.TAN -> Div(Expr.ONE, Pow(Call(Fn.COS, u), Expr.TWO))
        Fn.COT -> Neg(Div(Expr.ONE, Pow(Call(Fn.SIN, u), Expr.TWO)))
        Fn.ASIN -> Div(Expr.ONE, Call(Fn.SQRT, Sub(Expr.ONE, Pow(u, Expr.TWO))))
        Fn.ACOS -> Neg(Div(Expr.ONE, Call(Fn.SQRT, Sub(Expr.ONE, Pow(u, Expr.TWO)))))
        Fn.ATAN -> Div(Expr.ONE, Add(Expr.ONE, Pow(u, Expr.TWO)))
        Fn.SINH -> Call(Fn.COSH, u)
        Fn.COSH -> Call(Fn.SINH, u)
        Fn.TANH -> Div(Expr.ONE, Pow(Call(Fn.COSH, u), Expr.TWO))
        Fn.LN -> Div(Expr.ONE, u)
        Fn.LOG -> Div(Expr.ONE, Mul(u, Call(Fn.LN, Num(10.0))))
        Fn.SQRT -> Div(Expr.ONE, Mul(Expr.TWO, Call(Fn.SQRT, u)))
        Fn.EXP -> Call(Fn.EXP, u)
        Fn.ABS -> Div(u, Call(Fn.ABS, u)) // sign(u), undefined at u = 0
    }
}
