package com.example.plantillatallerlayout.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Evaluador de expresiones aritméticas con paréntesis y jerarquía de operaciones.
 *
 * Implementa un parser recursivo descendente sobre BigDecimal para evitar los
 * errores de coma flotante típicos (0.1 + 0.2 ≠ 0.3 en Double).
 *
 * Gramática:
 *   expresion := termino (('+'|'-') termino)*
 *   termino   := factor (('×'|'÷') factor)*
 *   factor    := numero | '(' expresion ')' | '-' factor | '+' factor
 */
object ExpressionEvaluator {

    private val MC: MathContext = MathContext(34, RoundingMode.HALF_EVEN)

    val OPERADORES: Set<Char> = setOf('+', '-', '×', '÷')

    sealed class Result {
        data class Ok(val valor: BigDecimal) : Result()
        object DivisionPorCero : Result()
        object Sintaxis : Result()
        object Vacia : Result()
    }

    fun evaluar(expresion: String): Result {
        if (expresion.isBlank()) return Result.Vacia
        return try {
            val tokens = tokenizar(expresion) ?: return Result.Sintaxis
            if (tokens.isEmpty()) return Result.Vacia
            val parser = Parser(tokens)
            val valor = parser.parseExpresion()
            if (parser.pos != tokens.size) Result.Sintaxis else Result.Ok(valor)
        } catch (_: ArithmeticException) {
            Result.DivisionPorCero
        } catch (_: Exception) {
            Result.Sintaxis
        }
    }

    private sealed class Token {
        data class Numero(val valor: BigDecimal) : Token()
        data class Operador(val simbolo: Char) : Token()
        object ParenAbre : Token()
        object ParenCierra : Token()
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
                    var puntos = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') puntos++
                        if (puntos > 1) return null
                        sb.append(expr[i++])
                    }
                    val texto = sb.toString()
                    if (texto == ".") return null
                    val v = try {
                        BigDecimal(texto, MC)
                    } catch (_: NumberFormatException) {
                        return null
                    }
                    tokens += Token.Numero(v)
                }
                c in OPERADORES -> { tokens += Token.Operador(c); i++ }
                c == '(' -> { tokens += Token.ParenAbre; i++ }
                c == ')' -> { tokens += Token.ParenCierra; i++ }
                else -> return null
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<Token>) {
        var pos = 0

        fun parseExpresion(): BigDecimal {
            var resultado = parseTermino()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Operador && (t.simbolo == '+' || t.simbolo == '-')) {
                    pos++
                    val derecho = parseTermino()
                    resultado = if (t.simbolo == '+') resultado.add(derecho, MC)
                                else resultado.subtract(derecho, MC)
                } else break
            }
            return resultado
        }

        private fun parseTermino(): BigDecimal {
            var resultado = parseFactor()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Operador && (t.simbolo == '×' || t.simbolo == '÷')) {
                    pos++
                    val derecho = parseFactor()
                    resultado = if (t.simbolo == '×') {
                        resultado.multiply(derecho, MC)
                    } else {
                        if (derecho.signum() == 0) throw ArithmeticException("÷0")
                        resultado.divide(derecho, MC)
                    }
                } else break
            }
            return resultado
        }

        private fun parseFactor(): BigDecimal {
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
                is Token.Operador -> when (t.simbolo) {
                    '-' -> { pos++; parseFactor().negate(MC) }
                    '+' -> { pos++; parseFactor() }
                    else -> throw IllegalStateException("Operador inesperado: ${t.simbolo}")
                }
                else -> throw IllegalStateException("Token inesperado")
            }
        }
    }
}
