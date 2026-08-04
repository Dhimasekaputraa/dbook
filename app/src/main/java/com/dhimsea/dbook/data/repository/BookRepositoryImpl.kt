package com.dhimsea.dbook.data.repository

import com.dhimsea.dbook.data.local.dao.AnnotationDao
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.entity.AnnotationEntity
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.data.local.entity.toDomainModel
import com.dhimsea.dbook.data.local.entity.toEntity
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val annotationDao : AnnotationDao
) : BookRepository{
    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getBookById(id: Long): Book? {
        return bookDao.getBooksById(id)?.toDomainModel()
    }

    override fun observeBookById(id: Long): Flow<Book?> {
        return bookDao.observeBookById(id).map { entity -> entity?.toDomainModel() }
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

    override suspend fun isBookExists(title: String, fileSize: Long): Boolean {
        return bookDao.isBookExists(title, fileSize)
    }

    override suspend fun updateReadingProgress(
        bookId: Long,
        page: Int,
        cfi: String?,
        progress: Float
    ) {
        bookDao.updateReadingProgress(bookId, page, cfi, progress)
    }

    override fun getAnnotationsForBook(bookId: Long): Flow<List<Annotation>> {
        return annotationDao.getAnnotationsForBook(bookId).map { entityList ->
        entityList.map { it.toDomain() } }
    }

    override suspend fun insertAnnotation(annotation: Annotation): Long {
        return annotationDao.insertAnnotation(annotation.toEntity())
    }

    override suspend fun deleteAnnotation(annotation: Annotation) {
        annotationDao.deleteAnnotation(annotation.toEntity())
    }

    private fun AnnotationEntity.toDomain() = Annotation(
        id = id,
        bookId = bookId,
        cfi = cfi,
        chapterName = chapterName,
        pageNumber = pageNumber,
        text = text,
        note = note,
        colorHex = colorHex,
        createdAt = createdAt
    )

    private fun Annotation.toEntity() = AnnotationEntity(
        id = id,
        bookId = bookId,
        cfi = cfi,
        chapterName = chapterName,
        pageNumber = pageNumber,
        text = text,
        note = note,
        colorHex = colorHex,
        createdAt = createdAt
    )
}