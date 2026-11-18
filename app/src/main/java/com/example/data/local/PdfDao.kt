package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY createdAt DESC")
    fun observeAllDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY lastOpenedAt DESC LIMIT 10")
    fun observeRecentDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE isFavorite = 1 ORDER BY lastOpenedAt DESC")
    fun observeFavoriteDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT COUNT(*) FROM pdf_documents")
    fun observeDocumentCount(): Flow<Int>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE filePath = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: PdfDocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: PdfDocumentEntity)

    @Delete
    suspend fun deleteDocument(doc: PdfDocumentEntity)

    @Query("UPDATE pdf_documents SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE pdf_documents SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pdf_documents SET name = :newName, filePath = :newPath WHERE id = :id")
    suspend fun renameDocument(id: Long, newName: String, newPath: String)

    @Query("SELECT * FROM pdf_bookmarks WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun observeBookmarks(documentId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM pdf_bookmarks WHERE documentId = :documentId AND pageIndex = :pageIndex")
    suspend fun removeBookmark(documentId: Long, pageIndex: Int)
}
