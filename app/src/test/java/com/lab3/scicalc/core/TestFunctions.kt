package com.lab3.scicalc.core

/** Test set of functions shared by the unit tests and the export for the Python (SymPy) check. */
object TestFunctions {
    val ALL = listOf(
        "x^3 - 2x + 1", "sin(x)*cos(x)", "x^x", "ln(x)/x", "exp(-x^2)",
        "tan(x)", "atan(x^2)", "sqrt(x^2+1)", "2^x", "x*ln(x) - x",
        "asin(x/2)", "acos(x/2)", "sinh(x)*cosh(x)", "tanh(x)", "log(x)",
        "1/(1+x^2)", "cot(x)", "abs(x-5)", "(x+1)^(1/2)", "e^(sin(x))",
    )
    val POINTS = listOf(0.3, 0.7, 1.1, 1.5)
}
