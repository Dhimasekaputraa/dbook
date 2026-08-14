package com.dhimsea.dbook.domain.repository

import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.Shelf
import com.dhimsea.dbook.domain.model.ShelfWithBooks
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    
    fun observeBookById(id: Long): Flow<Book?>
    suspend fun getBookByFilePath(filePath: String): Book?
    suspend fun getAllFilePaths(): List<String>
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun deleteBookByFilePath(filePath: String)
    suspend fun isBookExists(title: String, fileSize: Long): Boolean
    suspend fun updateReadingProgress(bookId: Long, page: Int, cfi: String?, progress: Float)
    
    fun getAnnotationsForBook(bookId: Long): Flow<List<Annotation>>
    suspend fun insertAnnotation(annotation: Annotation): Long
    suspend fun deleteAnnotation(annotation: Annotation)

    fun getAllShelvesWithBooks(): Flow<List<ShelfWithBooks>>
    suspend fun createShelfWithBooks(shelfName: String, selectedBookIds: List<Long>): Boolean
    suspend fun deleteShelf(shelfId: Long)
    suspend fun removeBookFromShelf(shelfId: Long, bookId: Long)
    suspend fun addBookToShelves(bookId: Long, shelfIds: List<Long>)
    suspend fun updateShelfWithBooks(shelfId: Long, newName: String, orderedBookIds: List<Long>): Boolean
}