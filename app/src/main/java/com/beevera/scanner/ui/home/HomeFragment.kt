package com.beevera.scanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beevera.scanner.R
import com.beevera.scanner.databinding.FragmentHomeBinding
import com.beevera.scanner.ui.history.DocumentAdapter
import com.beevera.scanner.viewmodel.DocumentViewModel
import java.util.*
import android.content.Context

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DocumentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Ícono de configuración → ir a Configuración
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.configFragment)
        }

        // Saludo según hora del día, sin nombre ni hora
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when {
            hour in 5..11  -> "¡Buenos días! "
            hour in 12..17 -> "¡Buenas tardes! "
            else           -> "¡Buenas noches! "
        }
        val nombre = requireContext()
            .getSharedPreferences("beevera_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""

        binding.tvGreeting.text = if (nombre.isNotEmpty()) "$saludo $nombre 👋" else "$saludo 👋"

        // RecyclerView de recientes
        // RecyclerView de recientes
        val adapter = DocumentAdapter(
            onLabelClick = {
                // Al tocar la etiqueta desde Home, vamos al historial
                findNavController().navigate(R.id.historyFragment)
            },
            onItemClick = {
                // Al tocar el documento desde Home, también vamos al historial
                findNavController().navigate(R.id.historyFragment)
            }
        )
        binding.recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecent.adapter = adapter

        // Observar todos los documentos
        viewModel.allDocuments.observe(viewLifecycleOwner) { docs ->

            binding.tvTotal.text = docs.size.toString()

            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            binding.tvToday.text = docs.count { it.date >= hoy }.toString()

            val inicioMes = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
            binding.tvThisMonth.text = docs.count { it.date >= inicioMes }.toString()

            binding.tvFacturas.text  = docs.count { it.label == "Facturas" }.toString()
            binding.tvContratos.text = docs.count { it.label == "Contratos" }.toString()
            binding.tvRecetas.text   = docs.count { it.label == "Recetas" }.toString()
            binding.tvOtros.text     = docs.count {
                it.label != "Facturas" && it.label != "Contratos" && it.label != "Recetas"
            }.toString()

            adapter.submitList(docs.take(3))
        }

        binding.tvVerTodos.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}