package com.beevera.scanner.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beevera.scanner.data.model.DocumentEntity
import com.beevera.scanner.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val onLabelClick: (DocumentEntity) -> Unit
) : ListAdapter<DocumentEntity, DocumentAdapter.DocViewHolder>(DiffCallback()) {

    inner class DocViewHolder(private val binding: ItemDocumentBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(doc: DocumentEntity) {
            binding.tvDocName.text = doc.name
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "MX"))
            val sizeKb = doc.size / 1024
            binding.tvDocMeta.text = "${sizeKb} KB · ${sdf.format(Date(doc.date))}"
            binding.tvDocIcon.text = if (doc.type == "PDF") "📄" else "🖼️"
            binding.tvDocLabel.text = doc.label

            // Color del badge según etiqueta
            val (bgColor, textColor) = when (doc.label) {
                "Facturas"  -> Pair("#EEF2FF", "#1A3A8F")
                "Contratos" -> Pair("#E6FAF7", "#0E7E6E")
                "Recetas"   -> Pair("#F3E8FF", "#7C3AED")
                "Otros"     -> Pair("#FEF9EC", "#D97706")
                else        -> Pair("#F1F5F9", "#64748B")
            }
            binding.tvDocLabel.background.setTint(Color.parseColor(bgColor))
            binding.tvDocLabel.setTextColor(Color.parseColor(textColor))

            binding.tvDocLabel.setOnClickListener { onLabelClick(doc) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DocViewHolder(ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<DocumentEntity>() {
        override fun areItemsTheSame(a: DocumentEntity, b: DocumentEntity) = a.id == b.id
        override fun areContentsTheSame(a: DocumentEntity, b: DocumentEntity) = a == b
    }
}