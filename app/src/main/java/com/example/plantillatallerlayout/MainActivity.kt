package com.example.plantillatallerlayout

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.plantillatallerlayout.calc.ExpressionEvaluator
import com.example.plantillatallerlayout.calc.NumberFormatter
import com.example.plantillatallerlayout.history.HistoryActivity
import com.example.plantillatallerlayout.history.HistoryManager
import com.google.android.material.button.MaterialButton
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {

    // Estado
    private var expresion: String = ""
    private var calculoFinalizado: Boolean = false

    // Vistas
    private lateinit var tvHistory: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    // Componentes
    private lateinit var historyManager: HistoryManager
    private lateinit var historyLauncher: ActivityResultLauncher<Intent>

    // Ciclo de vida
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHistory = findViewById(R.id.tvHistory)
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        historyManager = HistoryManager(this)
        historyLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val seleccionado = result.data?.getStringExtra(HistoryActivity.EXTRA_EXPRESION)
                if (!seleccionado.isNullOrBlank()) insertarDesdeHistorial(seleccionado)
            }
        }

        configurarBotones()
    }

    // Configuración de botones
    private fun configurarBotones() {
        mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        ).forEach { (id, digito) ->
            findViewById<MaterialButton>(id).setOnClickListener { alPulsarDigito(digito) }
        }

        mapOf(
            R.id.btnSum to '+',
            R.id.btnSub to '-',
            R.id.btnMult to '×',
            R.id.btnDiv to '÷'
        ).forEach { (id, op) ->
            findViewById<MaterialButton>(id).setOnClickListener { alPulsarOperador(op) }
        }

        findViewById<MaterialButton>(R.id.btnParen).setOnClickListener { alPulsarParentesis() }
        findViewById<MaterialButton>(R.id.btnDot).setOnClickListener { alPulsarPunto() }
        findViewById<MaterialButton>(R.id.btnPercent).setOnClickListener { alPulsarPorcentaje() }
        findViewById<MaterialButton>(R.id.btnSign).setOnClickListener { alPulsarCambioSigno() }
        findViewById<MaterialButton>(R.id.btnBackspace).setOnClickListener { alPulsarBorrar() }
        findViewById<MaterialButton>(R.id.btnC).setOnClickListener { limpiarTodo() }
        findViewById<MaterialButton>(R.id.btnEqual).setOnClickListener { alPulsarIgual() }

        findViewById<ImageButton>(R.id.btnAbrirHistorial).setOnClickListener {
            historyLauncher.launch(Intent(this, HistoryActivity::class.java))
        }
    }

    // Acciones de botones

    private fun alPulsarDigito(digito: String) {
        if (calculoFinalizado) {
            expresion = digito
            tvHistory.text = ""
            calculoFinalizado = false
        } else {
            expresion += digito
        }
        actualizarPantalla()
    }

    private fun alPulsarOperador(op: Char) {
        if (calculoFinalizado) {
            calculoFinalizado = false
            tvHistory.text = ""
        }

        if (expresion.isEmpty()) {
            if (op == '-') {
                expresion = "-"
                actualizarPantalla()
            }
            return
        }

        val ultimo = expresion.last()
        when {
            ultimo in ExpressionEvaluator.OPERADORES -> expresion = expresion.dropLast(1) + op
            ultimo.isDigit() || ultimo == '.' || ultimo == ')' -> expresion += op
            ultimo == '(' && op == '-' -> expresion += op
        }
        actualizarPantalla()
    }

    private fun alPulsarParentesis() {
        if (calculoFinalizado) {
            expresion = ""
            tvHistory.text = ""
            calculoFinalizado = false
        }

        val abiertos = expresion.count { it == '(' }
        val cerrados = expresion.count { it == ')' }
        val ultimo = expresion.lastOrNull()

        val abrir = when {
            expresion.isEmpty() -> true
            ultimo == '(' -> true
            ultimo in ExpressionEvaluator.OPERADORES -> true
            abiertos == cerrados -> true
            ultimo?.isDigit() == true || ultimo == '.' -> false
            ultimo == ')' -> false
            else -> true
        }

        if (abrir) {
            if (ultimo?.isDigit() == true || ultimo == '.' || ultimo == ')') expresion += "×"
            expresion += "("
        } else {
            val pendientes = abiertos - cerrados
            if (pendientes > 0 && ultimo != '(' && ultimo !in ExpressionEvaluator.OPERADORES) {
                expresion += ")"
            }
        }
        actualizarPantalla()
    }

    private fun alPulsarPunto() {
        if (calculoFinalizado) {
            expresion = "0."
            tvHistory.text = ""
            calculoFinalizado = false
            actualizarPantalla()
            return
        }
        if (expresion.isEmpty()) {
            expresion = "0."
            actualizarPantalla()
            return
        }

        val ultimoNumero = expresion.takeLastWhile { it.isDigit() || it == '.' }
        if (!ultimoNumero.contains('.')) {
            val ult = expresion.last()
            expresion += if (ult in ExpressionEvaluator.OPERADORES || ult == '(') "0." else "."
        }
        actualizarPantalla()
    }

    private fun alPulsarPorcentaje() {
        if (expresion.isEmpty()) return
        val ultimo = expresion.last()
        if (!ultimo.isDigit() && ultimo != ')') return

        val inicio = ultimoNumeroInicio()
        val ultimoNum = expresion.substring(inicio).toBigDecimalOrNull() ?: return
        val porcentaje = NumberFormatter.formatear(
            ultimoNum.divide(BigDecimal(100), java.math.MathContext(20))
        )
        expresion = expresion.substring(0, inicio) + porcentaje
        actualizarPantalla()
    }

    private fun alPulsarCambioSigno() {
        if (calculoFinalizado) {
            if (expresion.isEmpty()) return
            expresion = invertirSigno(expresion)
            tvHistory.text = ""
            calculoFinalizado = false
            actualizarPantalla()
            return
        }
        if (expresion.isEmpty()) return

        val inicio = ultimoNumeroInicio()
        val numero = expresion.substring(inicio)
        if (numero.isEmpty()) return

        val antes = expresion.substring(0, inicio)
        when {
            antes.endsWith("(-") && expresion.endsWith(")") ->
                expresion = antes.removeSuffix("(-") + numero.removeSuffix(")")
            antes.endsWith("(-") ->
                expresion = antes.removeSuffix("(-") + numero
            antes == "-" ->
                expresion = numero
            antes.isEmpty() ->
                expresion = "-$numero"
            else ->
                expresion = "$antes(-$numero)"
        }
        actualizarPantalla()
    }

    private fun invertirSigno(valor: String): String {
        val limpio = valor.trim()
        return when {
            limpio.startsWith("-") -> limpio.removePrefix("-")
            limpio.startsWith("(-") && limpio.endsWith(")") ->
                limpio.removePrefix("(-").removeSuffix(")")
            else -> "-$limpio"
        }
    }

    private fun alPulsarBorrar() {
        if (expresion.isEmpty()) return
        if (calculoFinalizado) {
            limpiarTodo()
            return
        }
        expresion = expresion.dropLast(1)
        actualizarPantalla()
    }

    private fun alPulsarIgual() {
        if (expresion.isEmpty() || calculoFinalizado) return

        val pendientes = expresion.count { it == '(' } - expresion.count { it == ')' }
        if (pendientes > 0) {
            val ult = expresion.lastOrNull()
            if (ult != null && (ult.isDigit() || ult == '.' || ult == ')')) {
                expresion += ")".repeat(pendientes)
            }
        }

        val ult = expresion.lastOrNull() ?: return
        if (ult in ExpressionEvaluator.OPERADORES || ult == '(' || expresion == "-") {
            tvExpression.text = expresion
            return
        }

        val resultado = when (val r = ExpressionEvaluator.evaluar(expresion)) {
            is ExpressionEvaluator.Result.Ok -> NumberFormatter.formatear(r.valor)
            is ExpressionEvaluator.Result.DivisionPorCero -> {
                tvResult.text = getString(R.string.error_div_zero)
                return
            }
            else -> {
                tvResult.text = getString(R.string.error_syntax)
                return
            }
        }

        val exprActual = expresion
        val historial = "$exprActual = $resultado"
        historyManager.agregar(exprActual, resultado)

        tvExpression.animate()
            .translationY(-80f)
            .scaleX(0.55f).scaleY(0.55f)
            .alpha(0f)
            .setDuration(280)
            .withEndAction {
                tvHistory.alpha = 0f
                tvHistory.text = historial
                tvHistory.animate().alpha(0.7f).setDuration(200).start()

                tvExpression.translationY = 0f
                tvExpression.scaleX = 1f
                tvExpression.scaleY = 1f
                tvExpression.text = resultado
                tvResult.text = ""
                tvExpression.animate().alpha(1f).setDuration(200).start()

                expresion = resultado
                calculoFinalizado = true
            }.start()
    }

    private fun limpiarTodo() {
        expresion = ""
        calculoFinalizado = false
        tvExpression.text = ""
        tvResult.text = ""
        tvHistory.text = ""
    }

    private fun insertarDesdeHistorial(valor: String) {
        if (calculoFinalizado) {
            expresion = valor
            calculoFinalizado = false
            tvHistory.text = ""
        } else if (expresion.isEmpty() ||
            expresion.last() in ExpressionEvaluator.OPERADORES ||
            expresion.last() == '('
        ) {
            expresion += valor
        } else {
            expresion = valor
        }
        actualizarPantalla()
    }

    private fun actualizarPantalla() {
        tvExpression.text = expresion

        if (expresion.isEmpty() ||
            calculoFinalizado ||
            expresion == "-" ||
            expresion.last() in ExpressionEvaluator.OPERADORES ||
            expresion.last() == '('
        ) {
            tvResult.text = ""
            return
        }

        when (val res = ExpressionEvaluator.evaluar(expresion)) {
            is ExpressionEvaluator.Result.Ok -> {
                val formateado = NumberFormatter.formatear(res.valor)
                tvResult.text = if (formateado == expresion) "" else "= $formateado"
            }
            is ExpressionEvaluator.Result.DivisionPorCero -> {
                tvResult.text = getString(R.string.error_div_zero)
            }
            is ExpressionEvaluator.Result.Sintaxis,
            is ExpressionEvaluator.Result.Vacia -> {
                tvResult.text = ""
            }
        }
    }

    // Utilidades

    private fun ultimoNumeroInicio(): Int {
        var i = expresion.length - 1
        while (i >= 0 && (expresion[i].isDigit() || expresion[i] == '.')) i--
        return i + 1
    }
}
