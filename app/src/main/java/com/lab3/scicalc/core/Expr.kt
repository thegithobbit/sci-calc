package com.lab3.scicalc.core

/**
 * Abstract syntax tree of a mathematical expression.
 * The tree is immutable; [Differentiator] and [Simplifier] build new trees.
 */
sealed class Expr {
    data class Num(val value: Double) : Expr()

    /** The single free variable `x`. */
    object X : Expr() {
        override fun toString() = "X"
    }

    /** Named constant (π, e) kept symbolic so it is printed nicely. */
    data class Const(val symbol: String, val value: Double) : Expr()

    data class Neg(val arg: Expr) : Expr()
    data class Add(val left: Expr, val right: Expr) : Expr()
    data class Sub(val left: Expr, val right: Expr) : Expr()
    data class Mul(val left: Expr, val right: Expr) : Expr()
    data class Div(val left: Expr, val right: Expr) : Expr()
    data class Pow(val base: Expr, val exponent: Expr) : Expr()
    data class Call(val fn: Fn, val arg: Expr) : Expr()
    data class Factorial(val arg: Expr) : Expr()

    /** True if the expression depends on `x`. */
    fun hasX(): Boolean = when (this) {
        is Num, is Const -> false
        X -> true
        is Neg -> arg.hasX()
        is Add -> left.hasX() || right.hasX()
        is Sub -> left.hasX() || right.hasX()
        is Mul -> left.hasX() || right.hasX()
        is Div -> left.hasX() || right.hasX()
        is Pow -> base.hasX() || exponent.hasX()
        is Call -> arg.hasX()
        is Factorial -> arg.hasX()
    }

    companion object {
        val PI = Const("π", Math.PI)
        val E = Const("e", Math.E)
        val ZERO = Num(0.0)
        val ONE = Num(1.0)
        val TWO = Num(2.0)
    }
}

/** Supported unary functions. [id] is the name used in input and output. */
enum class Fn(val id: String) {
    SIN("sin"), COS("cos"), TAN("tan"), COT("cot"),
    ASIN("asin"), ACOS("acos"), ATAN("atan"),
    SINH("sinh"), COSH("cosh"), TANH("tanh"),
    LN("ln"), LOG("log"), SQRT("sqrt"), EXP("exp"), ABS("abs");

    /** Trigonometric functions whose argument is an angle (affected by DEG/RAD). */
    val takesAngle: Boolean get() = this == SIN || this == COS || this == TAN || this == COT

    /** Inverse trigonometric functions that return an angle. */
    val returnsAngle: Boolean get() = this == ASIN || this == ACOS || this == ATAN

    companion object {
        private val byId = entries.associateBy { it.id }
        fun find(id: String): Fn? = byId[id]
    }
}

enum class AngleMode { DEG, RAD }
