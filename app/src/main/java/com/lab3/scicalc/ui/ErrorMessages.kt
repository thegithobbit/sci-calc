package com.lab3.scicalc.ui

import android.content.Context
import com.lab3.scicalc.R
import com.lab3.scicalc.core.CalcError
import com.lab3.scicalc.core.CalcException

/** Maps language-independent [CalcError] codes to localized messages. */
object ErrorMessages {

    fun of(context: Context, e: CalcException): String = when (e.error) {
        CalcError.EMPTY -> context.getString(R.string.err_empty)
        CalcError.UNEXPECTED_CHAR ->
            context.getString(R.string.err_unexpected_char, e.detail.orEmpty(), e.position + 1)
        CalcError.UNEXPECTED_END -> context.getString(R.string.err_unexpected_end)
        CalcError.MISMATCHED_PAREN -> context.getString(R.string.err_mismatched_paren)
        CalcError.UNKNOWN_FUNCTION -> context.getString(R.string.err_unknown_function, e.detail.orEmpty())
        CalcError.DIVISION_BY_ZERO -> context.getString(R.string.err_division_by_zero)
        CalcError.DOMAIN -> context.getString(R.string.err_domain)
        CalcError.OVERFLOW -> context.getString(R.string.err_overflow)
        CalcError.NOT_DIFFERENTIABLE -> context.getString(R.string.err_not_differentiable)
        CalcError.VARIABLE_NOT_ALLOWED -> context.getString(R.string.err_variable_not_allowed)
    }
}
