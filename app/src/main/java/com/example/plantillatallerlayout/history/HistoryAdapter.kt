package com.example.plantillatallerlayout.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plantillatallerlayout.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var datos: List<HistoryManager.Entrada>,
    private val onClick: (HistoryManager.Entrada) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val formatoFecha = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    fun actualizar(nuevos: List<HistoryManager.Entrada>) {
        datos = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = datos[position]
        holder.tvExpresion.text = item.expresion
        holder.tvResultado.text = "= ${item.resultado}"
        holder.tvFecha.text = formatoFecha.format(Date(item.timestamp))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = datos.size

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tvExpresion: TextView = v.findViewById(R.id.tvHistExpresion)
        val tvResultado: TextView = v.findViewById(R.id.tvHistResultado)
        val tvFecha: TextView = v.findViewById(R.id.tvHistFecha)
    }
}
