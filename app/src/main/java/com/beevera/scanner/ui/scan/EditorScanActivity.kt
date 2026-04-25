package com.beevera.scanner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.beevera.scanner.data.model.DocumentEntity
import com.beevera.scanner.databinding.ActivityEditorScanBinding
import com.beevera.scanner.utils.PdfGenerator
import com.beevera.scanner.viewmodel.DocumentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import android.os.Environment

class EditorScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorScanBinding
    private lateinit var viewModel: DocumentViewModel

    private val paginas = mutableListOf<File>()
    private var paginaActual = 0
    private var nombreDoc = "doc_${System.currentTimeMillis()}"
    private var rotacionActual = 0f

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var scanner: GmsDocumentScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        // Recibe las páginas desde ScanFragment
        val rutas = intent.getStringArrayListExtra("paginas") ?: arrayListOf()
        paginas.addAll(rutas.map { File(it) })

        if (paginas.isEmpty()) { finish(); return }

        mostrarPaginaActual()


        // ── Configura ML Kit para agregar páginas ─────────────────────────
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()

        scanner = GmsDocumentScanning.getClient(options)

        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val nuevasPaginas = scanResult?.pages ?: emptyList()

                if (nuevasPaginas.isNotEmpty()) {
                    nuevasPaginas.forEach { pagina ->
                        val uri = pagina.imageUri
                        val file = File(cacheDir, "scan_add_${System.currentTimeMillis()}.jpg")
                        contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        paginas.add(file) // ¡Se agrega a la lista actual!
                    }
                    paginaActual = paginas.size - 1 // Te mueve a la página recién agregada
                    rotacionActual = 0f
                    mostrarPaginaActual()
                }
            }
        }

        // ── Renombrar ──────────────────────────────────────────────
        binding.btnRenombrar.setOnClickListener {
            val input = EditText(this).apply {
                setText(nombreDoc)
                setPadding(48, 32, 48, 16)
            }
            AlertDialog.Builder(this)
                .setTitle("Cambiar nombre")
                .setView(input)
                .setPositiveButton("Guardar") { _, _ ->
                    val nuevo = input.text.toString().trim()
                    if (nuevo.isNotEmpty()) {
                        nombreDoc = nuevo
                        binding.tvNombreDoc.text = nombreDoc
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // ── Rotar ─────────────────────────────────────────────────
        binding.btnRotarIzq.setOnClickListener {
            rotacionActual -= 90f
            binding.ivDocumento.rotation = rotacionActual
        }

        // ── Eliminar página actual ─────────────────────────────────
        binding.btnEliminarPag.setOnClickListener {
            if (paginas.size == 1) {
                AlertDialog.Builder(this)
                    .setTitle("¿Cancelar escaneo?")
                    .setMessage("Solo tienes una página. ¿Deseas cancelar el escaneo?")
                    .setPositiveButton("Sí, cancelar") { _, _ -> finish() }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                paginas.removeAt(paginaActual)
                if (paginaActual >= paginas.size) paginaActual = paginas.size - 1
                rotacionActual = 0f
                mostrarPaginaActual()
                Toast.makeText(this, "Página eliminada", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Agregar página ─────────────────────
        binding.btnAgregarPag.setOnClickListener {
            scanner.getStartScanIntent(this)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al iniciar escáner: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ── Guardar PDF ────────────────────────────────────────────
        binding.btnGuardarPdf.setOnClickListener {
            guardarPdf()
        }
    }

    private fun mostrarPaginaActual() {
        if (paginas.isEmpty()) return
        val bitmap = BitmapFactory.decodeFile(paginas[paginaActual].absolutePath)
        binding.ivDocumento.setImageBitmap(bitmap)
        binding.ivDocumento.rotation = rotacionActual
        binding.tvPaginaActual.text = "Página ${paginaActual + 1} de ${paginas.size}"
        binding.tvNombreDoc.text = nombreDoc
    }

    private fun guardarPdf() {
        binding.btnGuardarPdf.isEnabled = false
        binding.btnGuardarPdf.text = "Guardando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Obtenemos la ruta a la carpeta pública de "Documentos" del dispositivo
                val carpetaDocumentos = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

                // 2. Opcional pero recomendado: Creamos una subcarpeta con el nombre de tu app
                val outputDir = File(carpetaDocumentos, "Beevera")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                // Aplica rotación real a los bitmaps antes de generar PDF
                val archivosFinales = paginas.mapIndexed { i, file ->
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    val matrix = Matrix().apply { postRotate(if (i == paginaActual) rotacionActual else 0f) }
                    val rotado = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    val tempFile = File(cacheDir, "final_$i.jpg")
                    tempFile.outputStream().use { rotado.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    tempFile
                }

                val nombreArchivo = if (nombreDoc.endsWith(".pdf")) nombreDoc else "$nombreDoc.pdf"
                val pdfFile = PdfGenerator.convertImagesToPdf(archivosFinales, outputDir, nombreArchivo)

                viewModel.insertDocument(DocumentEntity(
                    name  = pdfFile.name,
                    path  = pdfFile.absolutePath, // Ahora la ruta será en Documentos/Beevera/...
                    size  = pdfFile.length(),
                    date  = System.currentTimeMillis(),
                    type  = "PDF",
                    label = "Sin etiquetar"
                ))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditorScanActivity,
                        "✅ Guardado en Documentos/Beevera", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnGuardarPdf.isEnabled = true
                    binding.btnGuardarPdf.text = "GUARDAR PDF"
                    Toast.makeText(this@EditorScanActivity,
                        "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}