package com.beevera.scanner.ui.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.beevera.scanner.LoginActivity
import com.beevera.scanner.R
import com.beevera.scanner.databinding.FragmentConfigBinding
import com.google.firebase.auth.FirebaseAuth

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("beevera_prefs", Context.MODE_PRIVATE)
        // ── Botón de regresar ─────────────────────────────────────────
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // ── Datos del usuario ─────────────────────────────────────────
        val nombre = prefs.getString("user_name", "Usuario") ?: "Usuario"
        val email  = prefs.getString("user_email", "") ?: ""
        binding.tvNombreUser.text = nombre
        binding.tvEmailUser.text  = email
        binding.tvIniciales.text  = nombre.take(2).uppercase()

        // ── Efecto B/N — carga preferencia guardada ───────────────────
        binding.switchEfecto.isChecked = prefs.getBoolean("efecto_bn", true)
        binding.switchEfecto.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("efecto_bn", isChecked).apply()
        }

        // ── Recordatorios ─────────────────────────────────────────────
        binding.switchRecordatorios.isChecked = prefs.getBoolean("recordatorios", true)
        binding.switchRecordatorios.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("recordatorios", isChecked).apply()
        }

        // ── Alerta de almacenamiento ──────────────────────────────────
        binding.switchAlerta.isChecked = prefs.getBoolean("alerta_storage", false)
        binding.switchAlerta.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerta_storage", isChecked).apply()
        }
        // ── Carpeta de guardado ───────────────────────────────────────
        val carpetaActual = prefs.getString("carpeta_guardado", "Beevera (predeterminada)") ?: "Beevera (predeterminada)"
        binding.tvCarpetaActual.text = carpetaActual

        binding.btnCarpeta.setOnClickListener {
            val opciones = arrayOf("Beevera (predeterminada)", "Documentos", "Descargas")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Carpeta de guardado")
                .setSingleChoiceItems(opciones,
                    opciones.indexOf(carpetaActual).coerceAtLeast(0)
                ) { dialog, which ->
                    val seleccion = opciones[which]
                    prefs.edit().putString("carpeta_guardado", seleccion).apply()
                    binding.tvCarpetaActual.text = seleccion
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // ── Administrar etiquetas ─────────────────────────────────────
        binding.btnAdminEtiquetas.setOnClickListener {
            findNavController().navigate(R.id.etiquetasFragment)
        }

        // ── Cerrar sesión ─────────────────────────────────────────────
        binding.btnCerrarSesion.setOnClickListener {

            // 1. Cerramos la sesión en Firebase (La nube)
            FirebaseAuth.getInstance().signOut()

            // 2. Borramos los datos locales (El teléfono)
            prefs.edit()
                .remove("user_email")
                .remove("user_name")
                .apply()

            // 3. Lo mandamos a la pantalla de Login y borramos el historial de pantallas
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}