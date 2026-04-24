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
import kotlinx.coroutines.launch

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

    // ── Etiquetas personalizadas ──────────────────────────────────────
    // LiveData que cualquier Fragment puede observar
    private val _etiquetasPersonalizadas = MutableLiveData<List<String>>()
    val etiquetasPersonalizadas: LiveData<List<String>> = _etiquetasPersonalizadas

    init {
        cargarEtiquetasGuardadas()
    }

    private fun cargarEtiquetasGuardadas() {
        val guardadas = prefs.getStringSet("etiquetas_custom", emptySet()) ?: emptySet()
        _etiquetasPersonalizadas.value = guardadas.sorted()
    }

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

        // Documentos con esa etiqueta pasan a "Sin etiquetar"
        viewModelScope.launch {
            val docs = allDocuments.value?.filter { it.label == nombre } ?: return@launch
            docs.forEach { dao.insert(it.copy(label = "Sin etiquetar")) }
        }
    }

    // ── Todas las etiquetas (fijas + personalizadas) ──────────────────
    fun todasLasEtiquetas(): List<String> {
        val fijas = listOf("Facturas", "Contratos", "Recetas")
        val custom = prefs.getStringSet("etiquetas_custom", emptySet())?.toList() ?: emptyList()
        return (fijas + custom).distinct().sorted()
    }

    fun insertDocument(document: DocumentEntity) {
        viewModelScope.launch { dao.insert(document) }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch { dao.delete(document) }
    }

    fun filterByLabel(label: String?) { _selectedLabel.value = label }
    fun search(query: String) { _searchQuery.value = query }

    fun removeLabel(label: String) {
        eliminarEtiqueta(label)
    }
}