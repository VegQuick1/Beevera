package com.beevera.scanner.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.beevera.scanner.data.model.DocumentEntity

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY date DESC")
    fun getAllDocuments(): LiveData<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE label = :label ORDER BY date DESC")
    fun getByLabel(label: String): LiveData<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchDocuments(query: String): LiveData<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT SUM(size) FROM documents")
    fun getTotalSize(): LiveData<Long?>

    @Query("SELECT SUM(size) FROM documents WHERE label = :label")
    fun getSizeByLabel(label: String): LiveData<Long?>

    @Query("SELECT * FROM documents ORDER BY date DESC")
    suspend fun getAllDocumentsSync(): List<DocumentEntity>
}