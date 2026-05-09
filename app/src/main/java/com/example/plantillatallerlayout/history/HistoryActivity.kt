package com.example.plantillatallerlayout.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plantillatallerlayout.R

class HistoryActivity : AppCompatActivity() {

    private lateinit var manager: HistoryManager
    private lateinit var adapter: HistoryAdapter
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        manager = HistoryManager(this)
        tvVacio = findViewById(R.id.tvHistVacio)

        val rv = findViewById<RecyclerView>(R.id.rvHistorial)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(manager.obtenerTodas()) { entrada ->
            val data = Intent().apply {
                putExtra(EXTRA_EXPRESION, entrada.resultado)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnBorrarHistorial).setOnClickListener {
            manager.borrarTodo()
            adapter.actualizar(emptyList())
            actualizarVacio()
            Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }

        actualizarVacio()
    }

    private fun actualizarVacio() {
        val vacio = manager.obtenerTodas().isEmpty()
        tvVacio.visibility = if (vacio) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_EXPRESION = "extra_expresion"
    }
}
