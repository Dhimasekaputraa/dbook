package com.dhimsea.dbook.data.repository

import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.entity.toDomainModel
import com.dhimsea.dbook.data.local.entity.toEntity
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
    private val bookDao: BookDao
) : BookRepository{
    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getBookById(id: Long): Book? {
        return bookDao.getBooksById(id)?.toDomainModel()
    }

    override suspend fun getBookByFilePath(filePath: String): Book? {
        return bookDao.getBookByFilePath(filePath)?.toDomainModel()
    }

    override suspend fun getAllFilePaths(): List<String> {
        return bookDao.getAllFilePaths()
    }

    override suspend fun insertBook(book: Book): Long {
        return bookDao.insertBook(book.toEntity())
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(book.toEntity())
    }

    override suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book.toEntity())
    }

    override suspend fun deleteBookByFilePath(filePath: String) {
        bookDao.deleteBookByFilePath(filePath)
    }

    override suspend fun isFileSizeExists(fileSize: Long): Boolean {
        return bookDao.countBooksByFileSize(fileSize) > 0
    }

    override suspend fun updateReadingProgress(
        bookId: Long,
        page: Int,
        cfi: String?,
        progress: Float
    ) {
        bookDao.updateReadingProgress(bookId, page, cfi, progress)
    }
}