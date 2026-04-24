package com.beevera.scanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val size: Long,
    val date: Long,
    val type: String,
    val label: String = "Sin etiquetar"
)