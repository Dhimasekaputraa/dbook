package com.dhimsea.dbook.domain.repository

import com.dhimsea.dbook.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository{
    fun getAllBooks(): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun getBookByFilePath(filePath: String): Book?
    suspend fun getAllFilePaths(): List<String>
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun deleteBookByFilePath(filePath: String)
    suspend fun isFileSizeExists(fileSize: Long): Boolean
    suspend fun updateReadingProgress(bookId: Long, page: Int, cfi: String?, progress: Float)
}