package com.example.plantillatallerlayout

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // ── Estado ────────────────────────────────────────────────────────────────
    private var expresion: String = ""          // lo que se muestra en tvExpression
    private var calculoFinalizado: Boolean = false

    // Vistas
    private lateinit var tvHistory: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    // Constantes
    private val OPERADORES = setOf('+', '-', '×', '÷')

    // ── Ciclo de vida ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHistory    = findViewById(R.id.tvHistory)
        tvExpression = findViewById(R.id.tvExpression)
        tvResult     = findViewById(R.id.tvResult)

        configurarBotones()
    }

    // ── Configuración de botones ──────────────────────────────────────────────
    private fun configurarBotones() {
        // Dígitos
        val digitoIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )
        digitoIds.forEach { (id, digito) ->
            findViewById<Button>(id).setOnClickListener { alPulsarDigito(digito) }
        }

        // Operadores
        val operadorIds = mapOf(
            R.id.btnSum  to "+",
            R.id.btnSub  to "-",
            R.id.btnMult to "×",
            R.id.btnDiv  to "÷"
        )
        operadorIds.forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener { alPulsarOperador(op) }
        }

        // Paréntesis
        findViewById<Button>(R.id.btnParen).setOnClickListener { alPulsarParentesis() }

        // Resto
        findViewById<Button>(R.id.btnDot).setOnClickListener      { alPulsarPunto() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener   { alPulsarPorcentaje() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { alPulsarBorrar() }
        findViewById<Button>(R.id.btnC).setOnClickListener         { limpiarTodo() }
        findViewById<Button>(R.id.btnEqual).setOnClickListener     { alPulsarIgual() }
    }

    // ── Acciones de botones ───────────────────────────────────────────────────

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

    private fun alPulsarOperador(op: String) {
        if (expresion.isEmpty()) return

        if (calculoFinalizado) {
            expresion = expresion.removePrefix("=")
            calculoFinalizado = false
        }

        val ultimo = expresion.last()

        when {
            ultimo in OPERADORES -> expresion = expresion.dropLast(1) + op
            // [ARREGLO] Agregado el '.' para que se pueda pulsar un operador tras un decimal
            ultimo.isDigit() || ultimo == '.' || ultimo == ')' -> expresion += op
            ultimo == '(' && op == "-" -> expresion += op
        }
        actualizarPantalla()
    }

    private fun alPulsarParentesis() {
        if (calculoFinalizado) {
            expresion = ""
            tvHistory.text = ""
            calculoFinalizado = false
        }

        val abiertos  = expresion.count { it == '(' }
        val cerrados  = expresion.count { it == ')' }
        val ultimo    = expresion.lastOrNull()

        val abrirParen = when {
            expresion.isEmpty()        -> true
            ultimo == '('             -> true
            ultimo in OPERADORES      -> true
            // [ARREGLO] Si están balanceados, es hora de ABRIR uno nuevo
            abiertos == cerrados      -> true
            ultimo?.isDigit() == true || ultimo == '.' -> false
            ultimo == ')'             -> false
            else                      -> true
        }

        if (abrirParen) {
            // Multiplicación implícita agregada protección para puntos
            if (ultimo?.isDigit() == true || ultimo == '.' || ultimo == ')') expresion += "×"
            expresion += "("
        } else {
            val pendientes = abiertos - cerrados
            if (pendientes > 0 && ultimo != '(' && ultimo !in OPERADORES) {
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
            expresion += if (expresion.last() in OPERADORES || expresion.last() == '(') "0." else "."
        }
        actualizarPantalla()
    }

    private fun alPulsarPorcentaje() {
        if (expresion.isEmpty()) return
        val indiceInicio = ultimoNumerInicio()
        val ultimoNum = expresion.substring(indiceInicio).toDoubleOrNull() ?: return
        val porcentaje = formatearNumero(ultimoNum / 100.0)
        expresion = expresion.substring(0, indiceInicio) + porcentaje
        actualizarPantalla()
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
        val preview = tvResult.text.toString().removePrefix("=")
        // [ARREGLO] Protege contra "Error: ÷0" para que no lo sume a la expresión actual
        if (preview.isEmpty() || preview.startsWith("Error")) return

        val exprActual = expresion
        val historial  = "$exprActual = $preview"

        tvExpression.animate()
            .translationY(-80f)
            .scaleX(0.55f).scaleY(0.55f)
            .alpha(0f)
            .setDuration(280)
            .withEndAction {
                tvHistory.alpha = 0f
                tvHistory.text  = historial
                tvHistory.animate().alpha(0.6f).setDuration(200).start()

                tvExpression.translationY = 0f
                tvExpression.scaleX = 1f
                tvExpression.scaleY = 1f
                tvExpression.text   = "=$preview"
                tvResult.text       = ""
                tvExpression.animate().alpha(1f).setDuration(200).start()

                expresion          = "=$preview"
                calculoFinalizado  = true
            }.start()
    }

    private fun limpiarTodo() {
        expresion         = ""
        calculoFinalizado = false
        tvExpression.text = ""
        tvResult.text     = ""
        tvHistory.text    = ""
    }

    private fun actualizarPantalla() {
        tvExpression.text = expresion

        if (expresion.isEmpty() || expresion.startsWith("=") || expresion == "-") {
            tvResult.text = ""
            return
        }

        val resultado = evaluarExpresion(expresion)
        tvResult.text = when {
            resultado == null           -> ""
            resultado.isNaN()           -> ""
            resultado.isInfinite()      -> "Error: ÷0"
            else                        -> "=${formatearNumero(resultado)}"
        }
    }

    // ── Motor de evaluación ───────────────────────────────────────────────────

    private fun evaluarExpresion(expr: String): Double? {
        return try {
            val tokens = tokenizar(expr) ?: return null
            val parser = Parser(tokens)
            val resultado = parser.parseExpresion()
            if (parser.pos == tokens.size) resultado else null
        } catch (e: Exception) {
            null
        }
    }

    private sealed class Token {
        data class Numero(val valor: Double)   : Token()
        data class Operador(val simbolo: Char) : Token()
        object ParenAbre                       : Token()
        object ParenCierra                     : Token()
    }

    private fun tokenizar(expr: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i++])
                    }
                    val v = sb.toString().toDoubleOrNull() ?: return null
                    tokens += Token.Numero(v)
                }
                c == '+' || c == '-' || c == '×' || c == '÷' -> {
                    tokens += Token.Operador(c)
                    i++
                }
                c == '(' -> { tokens += Token.ParenAbre;   i++ }
                c == ')' -> { tokens += Token.ParenCierra; i++ }
                else -> return null
            }
        }
        return tokens
    }

    private inner class Parser(private val tokens: List<Token>) {
        var pos = 0

        fun parseExpresion(): Double {
            var resultado = parseTermino()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Operador && (t.simbolo == '+' || t.simbolo == '-')) {
                    pos++
                    val derecho = parseTermino()
                    resultado = if (t.simbolo == '+') resultado + derecho else resultado - derecho
                } else break
            }
            return resultado
        }

        private fun parseTermino(): Double {
            var resultado = parseFactor()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Operador && (t.simbolo == '×' || t.simbolo == '÷')) {
                    pos++
                    val derecho = parseFactor()
                    resultado = if (t.simbolo == '×') resultado * derecho
                    else resultado / derecho
                } else break
            }
            return resultado
        }

        private fun parseFactor(): Double {
            if (pos >= tokens.size) throw IllegalStateException("Expresión incompleta")
            return when (val t = tokens[pos]) {
                is Token.Numero -> { pos++; t.valor }
                Token.ParenAbre -> {
                    pos++
                    val resultado = parseExpresion()
                    if (pos >= tokens.size || tokens[pos] != Token.ParenCierra)
                        throw IllegalStateException("Falta ')'")
                    pos++
                    resultado
                }
                is Token.Operador -> {
                    if (t.simbolo == '-') {
                        pos++
                        -parseFactor()
                    } else throw IllegalStateException("Operador inesperado")
                }
                else -> throw IllegalStateException("Token inesperado: $t")
            }
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private fun formatearNumero(num: Double): String {
        if (num.isInfinite() || num.isNaN()) return "Error"
        return if (num % 1.0 == 0.0 && abs(num) < 1e12) {
            num.toLong().toString()
        } else {
            // [ARREGLO CRÍTICO] Forzamos Locale.US para que siempre renderice puntos (ej 2.5).
            // Si el teléfono está en español, imprimiría "2,5" y crashearía al re-evaluar la expresión
            String.format(Locale.US, "%.10f", num).trimEnd('0').trimEnd('.')
        }
    }

    private fun ultimoNumerInicio(): Int {
        var i = expresion.length - 1
        while (i >= 0 && (expresion[i].isDigit() || expresion[i] == '.')) i--
        return i + 1
    }
}