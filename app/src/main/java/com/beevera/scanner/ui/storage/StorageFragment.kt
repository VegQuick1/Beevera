package com.beevera.scanner.ui.storage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.beevera.scanner.databinding.FragmentStorageBinding
import com.beevera.scanner.viewmodel.DocumentViewModel

class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocumentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recalcula cuando cambian documentos O etiquetas
        viewModel.allDocuments.observe(viewLifecycleOwner) { docs ->
            actualizarUI(docs)
        }

        viewModel.etiquetasPersonalizadas.observe(viewLifecycleOwner) {
            viewModel.allDocuments.value?.let { docs -> actualizarUI(docs) }
        }
    }

    private fun actualizarUI(docs: List<com.beevera.scanner.data.model.DocumentEntity>) {

        // ── Tamaño total ──────────────────────────────────────────────
        val totalBytes = docs.sumOf { it.size }

        // ── Espacio real del dispositivo ──────────────────────────────
        val dir        = requireContext().getExternalFilesDir("Beevera") ?: requireContext().filesDir
        val libreBytes = dir.freeSpace
        val totalDisp  = dir.totalSpace
        val porcentaje = if (totalDisp > 0)
            ((totalBytes.toFloat() / totalDisp.toFloat()) * 100).toInt().coerceIn(0, 100)
        else 0

        // ── Header ────────────────────────────────────────────────────
        binding.tvUsado.text          = formatSize(totalBytes)
        binding.tvTotalEspacio.text   = "de ${formatSize(totalDisp)}"
        binding.tvResumenStorage.text = "${formatSize(totalBytes)} usados · ${formatSize(libreBytes)} libres"
        binding.pbStorage.progress    = porcentaje
        binding.tvPorcentaje.text     = "$porcentaje% utilizado · ${formatSize(libreBytes)} libres"

        // ── Tarjetas dispositivo / nube ───────────────────────────────
        binding.tvDispositivoSize.text     = formatSize(totalBytes)
        binding.tvDispositivoArchivos.text = "${docs.size} archivo${if (docs.size != 1) "s" else ""}"
        binding.tvNubeSize.text            = "0 MB"
        binding.tvNubeArchivos.text        = "Próximamente"

        // ── Categorías fijas ──────────────────────────────────────────
        val facturas  = docs.filter { it.label == "Facturas" }
        val contratos = docs.filter { it.label == "Contratos" }
        val recetas   = docs.filter { it.label == "Recetas" }

        val bytesF = facturas.sumOf  { it.size }
        val bytesC = contratos.sumOf { it.size }
        val bytesR = recetas.sumOf   { it.size }

        fun pct(b: Long) = if (totalBytes > 0)
            ((b.toFloat() / totalBytes) * 100).toInt() else 0

        binding.tvFacturasSize.text  = formatSize(bytesF)
        binding.tvFacturasInfo.text  = "${pct(bytesF)}%"
        binding.pbFacturas.progress  = pct(bytesF)

        binding.tvContratosSize.text = formatSize(bytesC)
        binding.tvContratosInfo.text = "${pct(bytesC)}%"
        binding.pbContratos.progress = pct(bytesC)

        binding.tvRecetasSize.text   = formatSize(bytesR)
        binding.tvRecetasInfo.text   = "${pct(bytesR)}%"
        binding.pbRecetas.progress   = pct(bytesR)

        // ── Etiquetas personalizadas dinámicas ────────────────────────
        // Obtiene etiquetas que NO son las 3 fijas ni "Sin etiquetar"
        val etiquetasFijas = setOf("Facturas", "Contratos", "Recetas", "Sin etiquetar")
        val etiquetasCustom = viewModel.todasLasEtiquetas()
            .filter { it !in etiquetasFijas }

        // Agrupa el resto de docs (etiquetas custom + sin etiquetar) en "Otros"
        val etiquetasConocidas = setOf("Facturas", "Contratos", "Recetas") + etiquetasCustom
        val otros = docs.filter { it.label !in etiquetasConocidas }
        val bytesO = otros.sumOf { it.size }

        binding.tvOtrosSize.text  = formatSize(bytesO)
        binding.tvOtrosInfo.text  = "${pct(bytesO)}%"
        binding.pbOtros.progress  = pct(bytesO)

        // ── Genera filas dinámicas para etiquetas personalizadas ──────
        // Primero limpia las filas dinámicas anteriores
        val contenedor = binding.layoutCategoriasCustom
        contenedor.removeAllViews()

        etiquetasCustom.forEach { etiqueta ->
            val docsEtiqueta = docs.filter { it.label == etiqueta }
            val bytes        = docsEtiqueta.sumOf { it.size }
            val pctEtiqueta  = pct(bytes)

            // Fila completa igual al diseño de Facturas/Contratos/Recetas
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                setPadding(
                    (20 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt(),
                    (20 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Emoji
            val emoji = TextView(requireContext()).apply {
                text     = "🏷"
                textSize = 18f
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (12 * resources.displayMetrics.density).toInt()
                }
                layoutParams = lp
            }

            // Columna derecha
            val columna = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            // Fila nombre + tamaño
            val filaNombre = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (4 * resources.displayMetrics.density).toInt()
                }
            }

            val tvNombre = TextView(requireContext()).apply {
                text      = etiqueta
                textSize  = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val tvTamanio = TextView(requireContext()).apply {
                text      = formatSize(bytes)
                textSize  = 12f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            filaNombre.addView(tvNombre)
            filaNombre.addView(tvTamanio)

            // Barra de progreso
            val pb = ProgressBar(requireContext(),
                null, android.R.attr.progressBarStyleHorizontal).apply {
                progress = pctEtiqueta
                max      = 100
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (5 * resources.displayMetrics.density).toInt()
                )
                layoutParams = lp
                progressTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.holo_purple
                )
                progressBackgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
                    requireContext(), android.R.color.darker_gray
                )
            }

            // Porcentaje
            val tvPct = TextView(requireContext()).apply {
                text      = "$pctEtiqueta%"
                textSize  = 11f
                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (2 * resources.displayMetrics.density).toInt()
                }
                layoutParams = lp
            }

            columna.addView(filaNombre)
            columna.addView(pb)
            columna.addView(tvPct)

            fila.addView(emoji)
            fila.addView(columna)
            contenedor.addView(fila)
        }

        // ── Sugerencia inteligente ────────────────────────────────────
        val sugerencia = when {
            facturas.size > 10 ->
                "Tienes ${facturas.size} facturas (${formatSize(bytesF)}). Considera archivarlas."
            contratos.size > 10 ->
                "Tienes ${contratos.size} contratos (${formatSize(bytesC)}). Considera archivarlos."
            porcentaje > 80 ->
                "Estás usando más del 80% del espacio. Libera archivos antiguos."
            else ->
                "Todos tus documentos están organizados. ¡Buen trabajo! 🎉"
        }
        binding.tvSugerencia.text = sugerencia
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024L                  -> "$bytes B"
        bytes < 1024L * 1024L          -> "${"%.1f".format(bytes / 1024f)} KB"
        bytes < 1024L * 1024L * 1024L  -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
        else                            -> "${"%.2f".format(bytes / (1024f * 1024f * 1024f))} GB"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}