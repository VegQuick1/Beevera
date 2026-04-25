package com.beevera.scanner.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.beevera.scanner.data.db.AppDatabase
import com.beevera.scanner.data.model.DocumentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.*

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).documentDao()
    private val prefs: SharedPreferences =
        application.getSharedPreferences("beevera_prefs", android.content.Context.MODE_PRIVATE)

    val allDocuments: LiveData<List<DocumentEntity>> = dao.getAllDocuments()

    private val _selectedLabel = MutableLiveData<String?>(null)
    val filteredDocuments: LiveData<List<DocumentEntity>> =
        _selectedLabel.switchMap { label ->
            if (label == null) dao.getAllDocuments()
            else dao.getByLabel(label)
        }

    private val _searchQuery = MutableLiveData<String>("")
    val searchResults: LiveData<List<DocumentEntity>> =
        _searchQuery.switchMap { query ->
            if (query.isBlank()) dao.getAllDocuments()
            else dao.searchDocuments(query)
        }

    val totalSize: LiveData<Long?> = dao.getTotalSize()

    private val _etiquetasPersonalizadas = MutableLiveData<List<String>>()
    val etiquetasPersonalizadas: LiveData<List<String>> = _etiquetasPersonalizadas

    init {
        cargarEtiquetasGuardadas()
    }

    private fun cargarEtiquetasGuardadas() {
        val guardadas = prefs.getStringSet("etiquetas_custom", emptySet()) ?: emptySet()
        _etiquetasPersonalizadas.value = guardadas.sorted()
    }

    // ─── LÓGICA DE SINCRONIZACIÓN CON FIREBASE ───
    fun sincronizarEstadisticasConNube() {
        // Ejecutamos en una corrutina para no trabar la app
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val db = FirebaseFirestore.getInstance()
            val nombre = prefs.getString("user_name", "Usuario") ?: "Usuario"

            // ── NUEVO: Consultamos directamente al DAO para tener el dato fresquecito ──
            val listaDocs = dao.getAllDocumentsSync() // Necesitaremos crear esta función abajo

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val inicioHoy = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val inicioMes = cal.timeInMillis

            val stats = hashMapOf(
                "nombre_usuario" to nombre,
                "email" to user.email,
                "escaneos_hoy" to listaDocs.count { it.date >= inicioHoy },
                "escaneos_mes" to listaDocs.count { it.date >= inicioMes },
                "escaneos_totales" to listaDocs.size,
                "ultima_actualizacion" to Timestamp.now()
            )

            db.collection("usuarios").document(user.uid).set(stats)
                .addOnSuccessListener {
                    // Aquí podrías poner un log para confirmar que se subió
                }
        }
    }

    fun insertDocument(document: DocumentEntity) {
        viewModelScope.launch {
            dao.insert(document)
            // Esperamos un momento a que Room actualice la lista y sincronizamos
            sincronizarEstadisticasConNube()
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            dao.delete(document)
            sincronizarEstadisticasConNube()
        }
    }

    // ── Etiquetas ──
    fun agregarEtiqueta(nombre: String) {
        val actuales = prefs.getStringSet("etiquetas_custom", emptySet())?.toMutableSet()
            ?: mutableSetOf()
        actuales.add(nombre)
        prefs.edit().putStringSet("etiquetas_custom", actuales).apply()
        _etiquetasPersonalizadas.value = actuales.sorted()
    }

    fun eliminarEtiqueta(nombre: String) {
        val actuales = prefs.getStringSet("etiquetas_custom", emptySet())?.toMutableSet()
            ?: mutableSetOf()
        actuales.remove(nombre)
        prefs.edit().putStringSet("etiquetas_custom", actuales).apply()
        _etiquetasPersonalizadas.value = actuales.sorted()

        viewModelScope.launch {
            val docs = allDocuments.value?.filter { it.label == nombre } ?: return@launch
            docs.forEach { dao.insert(it.copy(label = "Sin etiquetar")) }
        }
    }

    fun todasLasEtiquetas(): List<String> {
        val fijas = listOf("Facturas", "Contratos", "Recetas")
        val custom = prefs.getStringSet("etiquetas_custom", emptySet())?.toList() ?: emptyList()
        return (fijas + custom).distinct().sorted()
    }

    fun filterByLabel(label: String?) { _selectedLabel.value = label }
    fun search(query: String) { _searchQuery.value = query }
    fun removeLabel(label: String) { eliminarEtiqueta(label) }
}