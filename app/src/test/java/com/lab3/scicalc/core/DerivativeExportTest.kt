package com.lab3.scicalc.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Exports the app's derivatives to a TSV file so that they can be checked
 * against SymPy by tools/check_derivatives.py.
 *
 * Output: app/build/derivative-check/derivatives.tsv
 * (Gradle runs unit tests with the module directory as working directory.)
 */
class DerivativeExportTest {

    @Test fun exportDerivativesForSympyCheck() {
        val out = File("build/derivative-check/derivatives.tsv")
        out.parentFile?.mkdirs()
        out.bufferedWriter().use { w ->
            w.write("function\torder\tderivative\tx0\tvalue\n")
            for (f in TestFunctions.ALL) for (order in 1..DerivativeCalculator.MAX_ORDER) {
                val r = DerivativeCalculator.differentiate(f, order)
                for (x0 in TestFunctions.POINTS) {
                    val v = DerivativeCalculator.valueAt(r.derivative, x0)
                    w.write("$f\t$order\t${r.derivativeText}\t$x0\t${v.toBigDecimal().toPlainString()}\n")
                }
            }
        }
        assertTrue(out.length() > 0)
    }
}
