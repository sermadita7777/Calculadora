package com.example.plantillatallerlayout.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persiste el historial de operaciones en SharedPreferences como JSON.
 * Mantiene un máximo de [MAX_ENTRADAS] entradas.
 */
class HistoryManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Entrada(
        val expresion: String,
        val resultado: String,
        val timestamp: Long
    )

    fun obtenerTodas(): List<Entrada> {
        val raw = prefs.getString(KEY_HISTORIAL, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        Entrada(
                            expresion = obj.getString("expr"),
                            resultado = obj.getString("res"),
                            timestamp = obj.optLong("ts", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun agregar(expresion: String, resultado: String) {
        val actuales = obtenerTodas().toMutableList()
        actuales.add(0, Entrada(expresion, resultado, System.currentTimeMillis()))
        while (actuales.size > MAX_ENTRADAS) actuales.removeAt(actuales.lastIndex)
        guardar(actuales)
    }

    fun borrarTodo() {
        prefs.edit().remove(KEY_HISTORIAL).apply()
    }

    private fun guardar(lista: List<Entrada>) {
        val array = JSONArray()
        lista.forEach { e ->
            val obj = JSONObject().apply {
                put("expr", e.expresion)
                put("res", e.resultado)
                put("ts", e.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORIAL, array.toString()).apply()
    }

    companion object {
        private const val PREFS = "calc_history_prefs"
        private const val KEY_HISTORIAL = "historial_json"
        private const val MAX_ENTRADAS = 100
    }
}
