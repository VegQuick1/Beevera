package com.beevera.scanner.ui.history

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.beevera.scanner.R
import com.beevera.scanner.data.model.DocumentEntity
import com.beevera.scanner.databinding.FragmentHistoryBinding
import com.beevera.scanner.viewmodel.DocumentViewModel
import java.util.Calendar

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocumentViewModel by activityViewModels()
    private lateinit var adapter: DocumentAdapter

    private var filtroActivo = "Todos"
    private var todosLosDocs = listOf<DocumentEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DocumentAdapter { doc -> showLabelDialog(doc) }
        binding.recyclerDocuments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDocuments.adapter = adapter

        // ── Observa documentos ────────────────────────────────────────
        viewModel.allDocuments.observe(viewLifecycleOwner) { docs ->
            todosLosDocs = docs
            binding.tvStatDocs.text = docs.size.toString()
            reconstruirFiltros(docs)
            aplicarFiltro(filtroActivo, docs)
        }

        // ── Observa etiquetas personalizadas ──────────────────────────
        // Se dispara cuando el usuario crea/elimina una etiqueta en Config
        viewModel.etiquetasPersonalizadas.observe(viewLifecycleOwner) {
            reconstruirFiltros(todosLosDocs)
        }

        // ── Búsqueda ──────────────────────────────────────────────────
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                val base = filtrarPorFiltroActivo(todosLosDocs)
                if (query.isBlank()) showDocs(base)
                else showDocs(base.filter {
                    it.name.contains(query, ignoreCase = true)
                })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ── Botones fijos ─────────────────────────────────────────────
        binding.btnAll.setOnClickListener {
            filtroActivo = "Todos"
            aplicarFiltro("Todos", todosLosDocs)
        }
        binding.btnHoy.setOnClickListener {
            filtroActivo = "Hoy"
            aplicarFiltro("Hoy", todosLosDocs)
        }
    }

    // ── Reconstruye botones dinámicamente ────────────────────────────
    private fun reconstruirFiltros(docs: List<DocumentEntity>) {
        // Combina etiquetas guardadas en prefs + etiquetas que aparecen en documentos
        val etiquetasGuardadas = viewModel.todasLasEtiquetas()
        val etiquetasEnDocs    = docs.map { it.label }
            .filter { it.isNotBlank() && it != "Sin etiquetar" }
            .distinct()
        val todasEtiquetas = (etiquetasGuardadas + etiquetasEnDocs)
            .distinct()
            .sorted()

        // Actualiza contador
        binding.tvStatEtiquetas.text = todasEtiquetas.size.toString()

        // Elimina botones dinámicos anteriores
        val layout = binding.layoutFiltros
        val botonesAEliminar = mutableListOf<View>()
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child.id != R.id.btnAll && child.id != R.id.btnHoy) {
                botonesAEliminar.add(child)
            }
        }
        botonesAEliminar.forEach { layout.removeView(it) }

        // Agrega un botón por cada etiqueta
        todasEtiquetas.forEach { etiqueta ->
            val btn = Button(requireContext()).apply {
                text = etiqueta
                textSize = 12f
                backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    if (filtroActivo == etiqueta) android.R.color.holo_blue_dark
                    else android.R.color.darker_gray
                )
                setTextColor(
                    if (filtroActivo == etiqueta) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#475569")
                )
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    (36 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                setOnClickListener {
                    filtroActivo = etiqueta
                    aplicarFiltro(etiqueta, todosLosDocs)
                }
            }
            layout.addView(btn)
        }

        actualizarEstiloBotonesFijos()
    }

    private fun actualizarEstiloBotonesFijos() {
        val esTodos = filtroActivo == "Todos"
        val esHoy   = filtroActivo == "Hoy"

        binding.btnAll.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (esTodos) android.R.color.holo_blue_dark else android.R.color.darker_gray
        )
        binding.btnAll.setTextColor(
            if (esTodos) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#475569")
        )

        binding.btnHoy.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (esHoy) android.R.color.holo_blue_dark else android.R.color.darker_gray
        )
        binding.btnHoy.setTextColor(
            if (esHoy) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#475569")
        )
    }

    private fun aplicarFiltro(filtro: String, docs: List<DocumentEntity>) {
        filtroActivo = filtro
        actualizarEstiloBotonesFijos()
        showDocs(filtrarPorFiltroActivo(docs))
    }

    private fun filtrarPorFiltroActivo(docs: List<DocumentEntity>): List<DocumentEntity> {
        return when (filtroActivo) {
            "Todos" -> docs
            "Hoy" -> {
                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                docs.filter { it.date >= hoy }
            }
            else -> docs.filter { it.label == filtroActivo }
        }
    }

    private fun showDocs(docs: List<DocumentEntity>) {
        adapter.submitList(docs)
        if (docs.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerDocuments.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerDocuments.visibility = View.VISIBLE
        }
    }

    private fun showLabelDialog(doc: DocumentEntity) {
        val todasEtiquetas = (
                listOf("Sin etiquetar") + viewModel.todasLasEtiquetas()
                ).toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Asignar etiqueta a:\n${doc.name}")
            .setItems(todasEtiquetas) { _, which ->
                viewModel.insertDocument(doc.copy(label = todasEtiquetas[which]))
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}