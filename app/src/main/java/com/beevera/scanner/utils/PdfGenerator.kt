package com.beevera.scanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File

object PdfGenerator {

    // ── Efecto de escaneo ────────────────────────────────────────────
    private fun applyDocumentEffect(original: Bitmap): Bitmap {
        val grayscale = toGrayscale(original)
        return increaseContrast(grayscale, 1.6f)
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun increaseContrast(src: Bitmap, contrast: Float): Bitmap {
        val offset = -128 * (contrast - 1)
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, offset,
            0f, contrast, 0f, 0f, offset,
            0f, 0f, contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
        val result = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    // ── Una sola imagen → PDF (uso original, ahora con efecto) ───────
    fun convertImageToPdf(imageFile: File, outputDir: File): File {
        return convertImagesToPdf(listOf(imageFile), outputDir)
    }

    // ── Múltiples imágenes → PDF (función nueva) ─────────────────────
    fun convertImagesToPdf(
        imageFiles: List<File>,
        outputDir: File,
        nombreArchivo: String = "doc_${System.currentTimeMillis()}.pdf"
    ): File {
        val nombre = if (nombreArchivo.endsWith(".pdf")) nombreArchivo else "$nombreArchivo.pdf"
        val pdfFile = File(outputDir, nombre)

        val writer   = PdfWriter(pdfFile)
        val pdfDoc   = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(36f, 36f, 36f, 36f)

        val pageWidth  = PageSize.A4.width  - 72f
        val pageHeight = PageSize.A4.height - 72f

        imageFiles.forEachIndexed { index, imageFile ->
            val raw    = BitmapFactory.decodeFile(imageFile.absolutePath)
            val bitmap = applyDocumentEffect(raw)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageBytes = stream.toByteArray()

            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val imgWidth: Float
            val imgHeight: Float
            if (ratio > pageWidth / pageHeight) {
                imgWidth  = pageWidth
                imgHeight = pageWidth / ratio
            } else {
                imgHeight = pageHeight
                imgWidth  = pageHeight * ratio
            }

            val imageData = ImageDataFactory.create(imageBytes)
            val pdfImage  = Image(imageData).setWidth(imgWidth).setHeight(imgHeight)

            if (index > 0) document.getPdfDocument().addNewPage()
            document.add(pdfImage)
        }

        document.close()
        return pdfFile
    }
}