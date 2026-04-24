package com.beevera.scanner.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.beevera.scanner.EditorScanActivity
import com.beevera.scanner.databinding.FragmentScanBinding
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var scanner: GmsDocumentScanner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Configura ML Kit Document Scanner ─────────────────────────
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .build()

        scanner = GmsDocumentScanning.getClient(options)

        // ── Launcher que recibe el resultado del escáner ───────────────
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val paginas = scanResult?.pages ?: emptyList()

                if (paginas.isEmpty()) {
                    resetBtn()
                    return@registerForActivityResult
                }

                // Guarda cada página como archivo temporal
                val archivos = ArrayList<String>()
                paginas.forEach { pagina ->
                    val uri = pagina.imageUri
                    val file = File(requireContext().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    archivos.add(file.absolutePath)
                }

                // Abre el editor con las páginas escaneadas
                val intent = Intent(requireContext(), EditorScanActivity::class.java).apply {
                    putStringArrayListExtra("paginas", archivos)
                }
                startActivity(intent)
                resetBtn()

            } else {
                resetBtn()
                if (result.resultCode != Activity.RESULT_CANCELED) {
                    Toast.makeText(requireContext(), "❌ Error al escanear", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnCapture.setOnClickListener { iniciarEscaneo() }
    }

    private fun iniciarEscaneo() {
        binding.btnCapture.isEnabled = false
        binding.btnCapture.text = "Iniciando escáner..."
        binding.tvDeteccion.text = "Abriendo escáner..."

        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                resetBtn()
                Toast.makeText(requireContext(),
                    "❌ Error al iniciar escáner: ${e.message}",
                    Toast.LENGTH_LONG).show()
                Log.e(TAG, "Scanner error: ${e.message}")
            }
    }

    private fun resetBtn() {
        binding.btnCapture.isEnabled = true
        binding.btnCapture.text = "TOCAR PARA CAPTURAR"
        binding.tvDeteccion.text = "Apunta al documento"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ScanFragment"
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val REQUEST_CODE_PERMISSIONS = 10
    }
}