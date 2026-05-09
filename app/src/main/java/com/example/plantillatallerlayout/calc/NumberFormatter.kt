package com.example.plantillatallerlayout.calc

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formateo de números para mostrar en pantalla.
 *
 * Reglas:
 * - Notación decimal con punto para que el parser pueda re-leer el número.
 * - Sin ceros innecesarios al final.
 * - Notación científica cuando el número es demasiado grande o pequeño.
 */
object NumberFormatter {

    private const val MAX_DIGITS = 12
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val plain = DecimalFormat("0.##########", symbols)
    private val scientific = DecimalFormat("0.##########E0", symbols)

    fun formatear(valor: BigDecimal): String {
        val stripped = valor.stripTrailingZeros()
        val abs = stripped.abs()

        val esEntero = stripped.scale() <= 0 || stripped.remainder(BigDecimal.ONE).signum() == 0
        if (esEntero && abs.precision() - abs.scale() <= MAX_DIGITS) {
            return stripped.toBigInteger().toString()
        }

        val precisionTotal = stripped.toPlainString().replace("-", "").replace(".", "").length
        return if (abs >= BigDecimal("1E12") || (abs.signum() != 0 && abs < BigDecimal("1E-9")) || precisionTotal > MAX_DIGITS) {
            scientific.format(stripped).replace("E", "e")
        } else {
            plain.format(stripped)
        }
    }
}
