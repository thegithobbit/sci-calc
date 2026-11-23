package com.lab3.scicalc.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.lab3.scicalc.R
import com.lab3.scicalc.core.CalcException
import com.lab3.scicalc.core.DerivativeCalculator
import com.lab3.scicalc.core.Evaluator
import com.lab3.scicalc.core.NumberFormatter
import com.lab3.scicalc.core.Parser
import com.lab3.scicalc.databinding.ActivityDerivativeBinding
import timber.log.Timber

/** Symbolic derivative of f(x) of order 1..3 with optional evaluation at a point. */
class DerivativeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDerivativeBinding
    private var lastDerivativeText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDerivativeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        EXAMPLES.forEach { example ->
            val chip = Chip(this).apply {
                text = example
                setOnClickListener {
                    Timber.d("Example selected: %s", example)
                    binding.inputFunction.setText(example)
                    compute()
                }
            }
            binding.examples.addView(chip)
        }

        binding.btnCompute.setOnClickListener { compute() }
        binding.inputFunction.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { compute(); true } else false
        }
        binding.inputPoint.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { compute(); true } else false
        }
        binding.btnUseResult.setOnClickListener {
            lastDerivativeText?.let {
                Timber.d("Using derivative as new f(x): %s", it)
                binding.inputFunction.setText(it)
                binding.resultCard.visibility = View.GONE
            }
        }

        // EditTexts restore their own text; recompute the result card if it was shown.
        if (savedInstanceState?.getBoolean(KEY_SHOWN) == true) {
            binding.root.post { compute() }
        }
        Timber.d("DerivativeActivity created")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHOWN, binding.resultCard.visibility == View.VISIBLE)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun selectedOrder(): Int = when (binding.orderGroup.checkedButtonId) {
        R.id.order2 -> 2
        R.id.order3 -> 3
        else -> 1
    }

    private fun compute() {
        binding.layoutFunction.error = null
        binding.layoutPoint.error = null

        val input = binding.inputFunction.text?.toString().orEmpty()
        val order = selectedOrder()
        val started = SystemClock.elapsedRealtime()

        val result = try {
            DerivativeCalculator.differentiate(input, order)
        } catch (e: CalcException) {
            Timber.w("Differentiation failed for '%s': %s", input, e.message)
            binding.layoutFunction.error = ErrorMessages.of(this, e)
            binding.resultCard.visibility = View.GONE
            return
        }
        val elapsed = SystemClock.elapsedRealtime() - started
        Timber.i("d^%d/dx^%d [%s] = %s (%d ms)", order, order, result.functionText, result.derivativeText, elapsed)

        val primes = PRIMES[order - 1]
        lastDerivativeText = result.derivativeText
        binding.derivativeLabel.text = getString(R.string.result_derivative, primes)
        binding.derivativeText.text = result.derivativeText
        binding.timingText.text = resources.getQuantityString(R.plurals.computed_in_ms, elapsed.toInt(), elapsed.toInt())
        showValueAtPoint(result, order, primes)
        binding.resultCard.visibility = View.VISIBLE
    }

    private fun showValueAtPoint(result: DerivativeCalculator.Result, order: Int, primes: String) {
        binding.valueText.visibility = View.GONE
        binding.numericText.visibility = View.GONE

        val pointText = binding.inputPoint.text?.toString().orEmpty()
        if (pointText.isBlank()) return

        // x0 may be an expression without x, e.g. "π/4" or "2^0.5"
        val x0 = try {
            Evaluator().evaluate(Parser.parse(pointText))
        } catch (e: CalcException) {
            Timber.w("Invalid point '%s': %s", pointText, e.message)
            binding.layoutPoint.error = getString(R.string.err_invalid_point)
            return
        }

        binding.valueText.visibility = View.VISIBLE
        try {
            val value = DerivativeCalculator.valueAt(result.derivative, x0)
            binding.valueText.text = getString(R.string.result_value, primes, pointText, NumberFormatter.format(value))
            Timber.i("Derivative value at x0=%s: %s", pointText, value)
        } catch (e: CalcException) {
            Timber.w("Cannot evaluate derivative at %s: %s", pointText, e.message)
            binding.valueText.text = getString(R.string.result_value_error, pointText, ErrorMessages.of(this, e))
            return
        }

        runCatching { DerivativeCalculator.numericDerivative(result.function, x0, order) }
            .onSuccess {
                binding.numericText.text = getString(R.string.result_numeric, NumberFormatter.format(it))
                binding.numericText.visibility = View.VISIBLE
            }
            .onFailure { Timber.d("Numeric check skipped: %s", it.message) }
    }

    private companion object {
        const val KEY_SHOWN = "result_shown"
        val PRIMES = listOf("′", "″", "‴")
        val EXAMPLES = listOf("x^3 - 2x + 1", "sin(x^2)", "x^x", "ln(x)/x", "e^(-x^2)", "atan(x)", "√(1+x^2)")
    }
}
