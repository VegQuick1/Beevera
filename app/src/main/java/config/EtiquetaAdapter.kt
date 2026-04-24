package com.beevera.scanner.ui.config

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beevera.scanner.databinding.ItemEtiquetaBinding

class EtiquetaAdapter(
    private val onEliminar: (String) -> Unit
) : ListAdapter<Pair<String, Int>, EtiquetaAdapter.EtiquetaViewHolder>(DiffCallback()) {

    inner class EtiquetaViewHolder(private val binding: ItemEtiquetaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<String, Int>) {
            val (nombre, conteo) = item
            binding.tvNombreEtiqueta.text = nombre
            binding.tvConteoEtiqueta.text = "$conteo docs"
            
            binding.btnEliminarEtiqueta.setOnClickListener {
                onEliminar(nombre)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EtiquetaViewHolder {
        val binding = ItemEtiquetaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EtiquetaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EtiquetaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<String, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: Pair<String, Int>, newItem: Pair<String, Int>): Boolean {
            return oldItem == newItem
        }
    }
}
