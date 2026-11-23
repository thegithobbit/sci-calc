package com.lab3.scicalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EvaluatorTest {

    private fun calc(s: String, mode: AngleMode = AngleMode.RAD): String =
        NumberFormatter.format(Evaluator(angleMode = mode).evaluate(Parser.parse(s)))

    private fun assertError(s: String, expected: CalcError) {
        try {
            Evaluator().evaluate(Parser.parse(s))
            fail("Expected $expected for '$s'")
        } catch (e: CalcException) {
            assertEquals(expected, e.error)
        }
    }

    @Test fun precedence() {
        assertEquals("14", calc("2+3*4"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("512", calc("2^3^2"))     // right-associative
        assertEquals("-4", calc("-2^2"))        // -(2^2)
        assertEquals("0.25", calc("2^-2"))
        assertEquals("1", calc("10-6-3"))       // left-associative
    }

    @Test fun floatingPointNoiseIsHidden() {
        assertEquals("0.3", calc("0.1+0.2"))
    }

    @Test fun unicodeKeypadSymbols() {
        assertEquals("2", calc("6÷3"))
        assertEquals("6", calc("2×3"))
        assertEquals("-1", calc("2−3"))
        assertEquals("3", calc("√9"))
        assertEquals("9", calc("3²"))
    }

    @Test fun implicitMultiplication() {
        assertEquals("6.28318530718", calc("2π"))
        assertEquals("-3", calc("(1+2)(3-4)"))
    }

    @Test fun scientificFunctions() {
        assertEquals("1", calc("sin(90)", AngleMode.DEG))
        assertEquals("0", calc("sin(180)", AngleMode.DEG))
        assertEquals("0", calc("cos(90)", AngleMode.DEG))
        assertEquals("1", calc("sin(π/2)"))
        assertEquals("1", calc("tan(45)", AngleMode.DEG))
        assertEquals("30", calc("asin(0.5)", AngleMode.DEG))
        assertEquals("1", calc("ln(e)"))
        assertEquals("3", calc("log(1000)"))
        assertEquals("120", calc("5!"))
        assertEquals("-2", calc("(-8)^(1/3)"))
        assertEquals("1500", calc("1.5E3"))
        assertEquals("16.3096909708", calc("2e3"))   // 2·e·3, lowercase e is a constant
    }

    @Test fun errors() {
        assertError("1/0", CalcError.DIVISION_BY_ZERO)
        assertError("sqrt(-1)", CalcError.DOMAIN)
        assertError("ln(0)", CalcError.DOMAIN)
        assertError("(-1)!", CalcError.DOMAIN)
        assertError("2*x", CalcError.VARIABLE_NOT_ALLOWED)
    }

    @Test fun parseErrors() {
        assertParseError("", CalcError.EMPTY)
        assertParseError("(2+3", CalcError.MISMATCHED_PAREN)
        assertParseError("2+3)", CalcError.MISMATCHED_PAREN)
        assertParseError("2+", CalcError.UNEXPECTED_END)
        assertParseError("foo(2)", CalcError.UNKNOWN_FUNCTION)
        assertParseError("2 # 3", CalcError.UNEXPECTED_CHAR)
    }

    private fun assertParseError(s: String, expected: CalcError) {
        try {
            Parser.parse(s)
            fail("Expected $expected for '$s'")
        } catch (e: CalcException) {
            assertEquals(expected, e.error)
        }
    }
}
