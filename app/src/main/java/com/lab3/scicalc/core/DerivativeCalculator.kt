package com.lab3.scicalc.core

/**
 * Facade used by the UI: parses f(x), differentiates it symbolically
 * and evaluates the result at a point. Also provides a numeric
 * finite-difference estimate, used to cross-check the symbolic answer.
 */
object DerivativeCalculator {

    const val MAX_ORDER = 3

    data class Result(
        val function: Expr,
        val derivative: Expr,
        val functionText: String,
        val derivativeText: String,
    )

    fun differentiate(input: String, order: Int): Result {
        require(order in 1..MAX_ORDER) { "order must be in 1..$MAX_ORDER" }
        val f = Parser.parse(input)
        val df = Differentiator.derivative(f, order)
        return Result(
            function = f,
            derivative = df,
            functionText = ExprPrinter.print(Simplifier.simplify(f)),
            derivativeText = ExprPrinter.print(df),
        )
    }

    /** Value of an expression at x = x0 (radians). */
    fun valueAt(e: Expr, x0: Double): Double = Evaluator(x = x0, angleMode = AngleMode.RAD).evaluate(e)

    /** Central finite differences of order 1..3. */
    fun numericDerivative(f: Expr, x0: Double, order: Int): Double {
        val ev = { x: Double -> valueAt(f, x) }
        return when (order) {
            1 -> { val h = 1e-6; (ev(x0 + h) - ev(x0 - h)) / (2 * h) }
            2 -> { val h = 1e-4; (ev(x0 + h) - 2 * ev(x0) + ev(x0 - h)) / (h * h) }
            3 -> {
                val h = 1e-3
                (ev(x0 + 2 * h) - 2 * ev(x0 + h) + 2 * ev(x0 - h) - ev(x0 - 2 * h)) / (2 * h * h * h)
            }
            else -> throw IllegalArgumentException("order must be in 1..$MAX_ORDER")
        }
    }
}
