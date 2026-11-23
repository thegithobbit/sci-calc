package com.lab3.scicalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class DerivativeTest {

    private fun d(s: String, order: Int = 1) = DerivativeCalculator.differentiate(s, order).derivativeText

    @Test fun basicRules() {
        assertEquals("0", d("5"))
        assertEquals("1", d("x"))
        assertEquals("3x^2", d("x^3"))
        assertEquals("6x + 2", d("3x^2 + 2x + 1"))
        assertEquals("cos(x)", d("sin(x)"))
        assertEquals("-sin(x)", d("cos(x)"))
        assertEquals("1/x", d("ln(x)"))
        assertEquals("exp(x)", d("exp(x)"))
        assertEquals("e^x", d("e^x"))
        assertEquals("1/(2√(x))", d("sqrt(x)"))
    }

    @Test fun chainRule() {
        assertEquals("2x·cos(x^2)", d("sin(x^2)"))
        assertEquals("3exp(3x)", d("exp(3x)"))
    }

    @Test fun higherOrder() {
        assertEquals("6x", d("x^3", 2))
        assertEquals("6", d("x^3", 3))
        assertEquals("-sin(x)", d("sin(x)", 2))
        assertEquals("-cos(x)", d("sin(x)", 3))
    }

    @Test fun symbolicOutputCanBeParsedAgain() {
        val inputs = listOf("x^x", "tan(x)/x", "asin(x)*ln(x)", "sqrt(1+x^2)", "log(x^2+1)")
        for (s in inputs) {
            val text = d(s)
            Parser.parse(text) // must not throw
        }
    }

    /** Main correctness check: symbolic derivative must agree with finite differences. */
    @Test fun symbolicMatchesNumeric() {
        val functions = TestFunctions.ALL
        val points = TestFunctions.POINTS
        for (f in functions) for (order in 1..3) for (x0 in points) {
            val r = DerivativeCalculator.differentiate(f, order)
            val symbolic = DerivativeCalculator.valueAt(r.derivative, x0)
            val numeric = DerivativeCalculator.numericDerivative(r.function, x0, order)
            val tol = when (order) { 1 -> 1e-5; 2 -> 1e-3; else -> 5e-2 } * max(1.0, abs(symbolic))
            if (abs(symbolic - numeric) > tol) {
                fail("f=$f order=$order x0=$x0: symbolic=$symbolic (${r.derivativeText}) numeric=$numeric")
            }
        }
    }

    @Test fun factorialOfXIsNotDifferentiable() {
        try {
            d("x!")
            fail()
        } catch (e: CalcException) {
            assertEquals(CalcError.NOT_DIFFERENTIABLE, e.error)
        }
    }
}
