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

class EditorScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorScanBinding
    private lateinit var viewModel: DocumentViewModel

    private val paginas = mutableListOf<File>()
    private var paginaActual = 0
    private var nombreDoc = "doc_${System.currentTimeMillis()}"
    private var rotacionActual = 0f

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

        // ── Agregar página → regresa a cámara ─────────────────────
        binding.btnAgregarPag.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("ir_a_scan", true)
                putStringArrayListExtra("paginas_previas", ArrayList(paginas.map { it.absolutePath }))
            }
            startActivity(intent)
            finish()
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
                val outputDir = getExternalFilesDir("Beevera") ?: filesDir
                outputDir.mkdirs()

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
                    path  = pdfFile.absolutePath,
                    size  = pdfFile.length(),
                    date  = System.currentTimeMillis(),
                    type  = "PDF",
                    label = "Sin etiquetar"
                ))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditorScanActivity,
                        "✅ PDF guardado: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
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