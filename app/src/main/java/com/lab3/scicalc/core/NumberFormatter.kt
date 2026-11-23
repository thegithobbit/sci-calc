package com.lab3.scicalc.core

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Formats doubles for display: removes floating point noise like 0.30000000000000004. */
object NumberFormatter {

    private const val SIGNIFICANT_DIGITS = 12

    fun format(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        if (v == 0.0) return "0"            // also turns -0.0 into 0
        if (abs(v) < 1e-14) return "0"      // rounding noise, e.g. sin(π)
        val rounded = BigDecimal(v).round(MathContext(SIGNIFICANT_DIGITS)).stripTrailingZeros()
        val exponent = rounded.precision() - rounded.scale() - 1
        return if (exponent in -6..14) {
            rounded.toPlainString()
        } else {
            rounded.toString().replace("E+", "E")
        }
    }
}
