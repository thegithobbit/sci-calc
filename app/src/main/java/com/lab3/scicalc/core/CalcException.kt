package com.lab3.scicalc.core

/**
 * Error kinds produced by the math core.
 * The core never contains user-visible text: the UI maps every kind
 * to a localized string resource, so errors are translated too (i18n).
 */
enum class CalcError {
    EMPTY,
    UNEXPECTED_CHAR,
    UNEXPECTED_END,
    MISMATCHED_PAREN,
    UNKNOWN_FUNCTION,
    DIVISION_BY_ZERO,
    DOMAIN,
    OVERFLOW,
    NOT_DIFFERENTIABLE,
    VARIABLE_NOT_ALLOWED,
}

/**
 * @param detail optional fragment of input (a symbol or a function name) for the message.
 * @param position 0-based position in the input, or -1 if unknown.
 */
class CalcException(
    val error: CalcError,
    val detail: String? = null,
    val position: Int = -1,
) : Exception("$error${detail?.let { " '$it'" } ?: ""}${if (position >= 0) " at $position" else ""}")
