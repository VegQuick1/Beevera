package com.beevera.scanner.ui.config

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.beevera.scanner.databinding.FragmentEtiquetasBinding
import com.beevera.scanner.viewmodel.DocumentViewModel

class EtiquetasFragment : Fragment() {

    private var _binding: FragmentEtiquetasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocumentViewModel by activityViewModels()

    private val etiquetasFijas = listOf("Facturas", "Contratos", "Recetas")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEtiquetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackEtiquetas.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val adapter = EtiquetaAdapter(
            onEliminar = { etiqueta ->
                if (etiquetasFijas.contains(etiqueta)) {
                    Toast.makeText(requireContext(),
                        "\"$etiqueta\" es una etiqueta del sistema y no se puede eliminar",
                        Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar etiqueta")
                        .setMessage("¿Eliminar \"$etiqueta\"? Los documentos quedarán sin etiquetar.")
                        .setPositiveButton("Eliminar") { _, _ ->
                            viewModel.eliminarEtiqueta(etiqueta)
                            Toast.makeText(requireContext(),
                                "Etiqueta eliminada", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        )

        binding.rvEtiquetas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEtiquetas.adapter = adapter

        // ── Observa etiquetas personalizadas + documentos ─────────────
        fun actualizarLista() {
            val todasEtiquetas = viewModel.todasLasEtiquetas()
            val docs = viewModel.allDocuments.value ?: emptyList()
            val conConteo = todasEtiquetas.map { etiqueta ->
                Pair(etiqueta, docs.count { it.label == etiqueta })
            }
            adapter.submitList(conConteo)
        }

        viewModel.etiquetasPersonalizadas.observe(viewLifecycleOwner) { actualizarLista() }
        viewModel.allDocuments.observe(viewLifecycleOwner) { actualizarLista() }

        // ── Crear nueva etiqueta ──────────────────────────────────────
        binding.btnCrearEtiqueta.setOnClickListener {
            val input = EditText(requireContext()).apply {
                hint = "Nombre de la etiqueta"
                setPadding(48, 32, 48, 16)
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Nueva etiqueta")
                .setView(input)
                .setPositiveButton("Crear") { _, _ ->
                    val nombre = input.text.toString().trim()
                        .replaceFirstChar { it.uppercase() }
                    when {
                        nombre.isEmpty() ->
                            Toast.makeText(requireContext(),
                                "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                        viewModel.todasLasEtiquetas().contains(nombre) ->
                            Toast.makeText(requireContext(),
                                "Esa etiqueta ya existe", Toast.LENGTH_SHORT).show()
                        else -> {
                            viewModel.agregarEtiqueta(nombre)
                            Toast.makeText(requireContext(),
                                "✅ Etiqueta \"$nombre\" creada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}