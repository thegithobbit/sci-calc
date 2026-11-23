package com.lab3.scicalc.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.lab3.scicalc.R
import com.lab3.scicalc.core.AngleMode
import com.lab3.scicalc.core.CalcException
import com.lab3.scicalc.core.Evaluator
import com.lab3.scicalc.core.NumberFormatter
import com.lab3.scicalc.core.Parser
import com.lab3.scicalc.databinding.ActivityMainBinding
import timber.log.Timber

/** Scientific calculator screen. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var expression = ""
    private var angleMode = AngleMode.DEG
    /** True right after "=", so the next digit starts a new expression. */
    private var justEvaluated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        savedInstanceState?.let {
            expression = it.getString(KEY_EXPRESSION, "")
            angleMode = AngleMode.valueOf(it.getString(KEY_ANGLE, AngleMode.DEG.name))
            justEvaluated = it.getBoolean(KEY_JUST_EVALUATED)
            Timber.d("State restored: expression='%s', angle=%s", expression, angleMode)
        }

        setupKeypad()
        render()
        Timber.d("MainActivity created")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_EXPRESSION, expression)
        outState.putString(KEY_ANGLE, angleMode.name)
        outState.putBoolean(KEY_JUST_EVALUATED, justEvaluated)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_derivative -> {
            Timber.i("Opening derivative screen")
            startActivity(Intent(this, DerivativeActivity::class.java))
            true
        }
        R.id.action_language -> {
            LanguagePicker.show(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupKeypad() {
        // Every key that has a tag simply inserts that text.
        binding.keypad.children.filterIsInstance<MaterialButton>().forEach { button ->
            val insert = button.tag as? String ?: return@forEach
            button.setOnClickListener { onInsert(insert) }
        }
        binding.btnClear.setOnClickListener { onClear() }
        binding.btnBackspace.setOnClickListener { onBackspace() }
        binding.btnBackspace.setOnLongClickListener { onClear(); true }
        binding.btnEquals.setOnClickListener { onEquals() }
        binding.btnAngle.setOnClickListener { onToggleAngle() }
    }

    private fun onInsert(text: String) {
        if (justEvaluated) {
            // After "=": a digit/function starts over, an operator continues with the result.
            val continues = text.first() in "+−×÷^!" || text == "^2"
            if (!continues) expression = ""
            justEvaluated = false
        }
        expression += text
        Timber.v("Key '%s' -> expression '%s'", text, expression)
        render()
    }

    private fun onBackspace() {
        if (expression.isEmpty()) return
        // remove a whole function name like "sin(" at once
        val fn = FUNCTION_TAIL.find(expression)
        expression = if (fn != null) expression.substring(0, fn.range.first) else expression.dropLast(1)
        justEvaluated = false
        render()
    }

    private fun onClear() {
        Timber.d("Clear")
        expression = ""
        justEvaluated = false
        render()
    }

    private fun onToggleAngle() {
        angleMode = if (angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        Timber.i("Angle mode switched to %s", angleMode)
        render()
    }

    private fun onEquals() {
        if (expression.isBlank()) return
        try {
            val value = evaluate(expression)
            val formatted = NumberFormatter.format(value)
            Timber.i("Evaluated '%s' [%s] = %s", expression, angleMode, formatted)
            expression = formatted
            justEvaluated = true
            render()
        } catch (e: CalcException) {
            Timber.w("Evaluation failed for '%s': %s", expression, e.message)
            binding.result.text = ErrorMessages.of(this, e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while evaluating '%s'", expression)
        }
    }

    private fun evaluate(text: String): Double =
        Evaluator(x = null, angleMode = angleMode).evaluate(Parser.parse(text))

    /** Redraws the display and shows a live preview of the result. */
    private fun render() {
        binding.expression.text = expression
        binding.expressionScroll.post { binding.expressionScroll.fullScroll(View.FOCUS_RIGHT) }

        val angleLabel = getString(if (angleMode == AngleMode.DEG) R.string.angle_deg else R.string.angle_rad)
        binding.angleIndicator.text = angleLabel
        binding.btnAngle.text = angleLabel
        binding.btnAngle.contentDescription = getString(R.string.cd_angle_mode, angleLabel)

        binding.result.text = if (justEvaluated || expression.isBlank()) {
            ""
        } else {
            // the preview is silent: an unfinished expression is not an error yet
            runCatching { "= " + NumberFormatter.format(evaluate(expression)) }.getOrDefault("")
        }
    }

    private companion object {
        const val KEY_EXPRESSION = "expression"
        const val KEY_ANGLE = "angle"
        const val KEY_JUST_EVALUATED = "just_evaluated"
        val FUNCTION_TAIL = Regex("([a-z]+|√)\\($")
    }
}
