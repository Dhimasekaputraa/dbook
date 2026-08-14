package com.dhimsea.dbook.data.repository

import com.dhimsea.dbook.data.local.dao.AnnotationDao
import com.dhimsea.dbook.data.local.dao.BookDao
import com.dhimsea.dbook.data.local.dao.ShelfDao
import com.dhimsea.dbook.data.local.entity.AnnotationEntity
import com.dhimsea.dbook.data.local.entity.ShelfBookCrossRef
import com.dhimsea.dbook.data.local.entity.toDomainModel
import com.dhimsea.dbook.data.local.entity.toEntity
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.Shelf
import com.dhimsea.dbook.domain.model.ShelfWithBooks
import com.dhimsea.dbook.data.local.entity.ShelfEntity
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val annotationDao : AnnotationDao,
    private val shelfDao: ShelfDao
) : BookRepository {

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
            entityList.map { it.toDomain() } 
        }
    }

    override suspend fun insertAnnotation(annotation: Annotation): Long {
        return annotationDao.insertAnnotation(annotation.toEntity())
    }

    override suspend fun deleteAnnotation(annotation: Annotation) {
        annotationDao.deleteAnnotation(annotation.toEntity())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllShelvesWithBooks(): Flow<List<ShelfWithBooks>> {
        return shelfDao.getAllShelves().flatMapLatest { shelfEntities ->
            if (shelfEntities.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = shelfEntities.map { shelfEntity ->
                    shelfDao.getBooksForShelf(shelfEntity.id).map { bookEntities ->
                        ShelfWithBooks(
                            shelf = shelfEntity.toDomainModel(),
                            books = bookEntities.map { it.toDomainModel() }
                        )
                    }
                }
                combine(flows) { array -> array.toList() }
            }
        }
    }

    override suspend fun createShelfWithBooks(shelfName: String, selectedBookIds: List<Long>): Boolean {
        val trimmedName = shelfName.trim()
        val existing = shelfDao.getShelfByName(trimmedName)
        if (existing != null) return false

        if (selectedBookIds.isEmpty()) return false

        val newShelf = Shelf(name = trimmedName)
        val shelfId = shelfDao.insertShelf(newShelf.toEntity())

        selectedBookIds.forEach { bookId ->
            shelfDao.insertBookToShelf(ShelfBookCrossRef(shelfId, bookId))
        }
        return true
    }

    override suspend fun deleteShelf(shelfId: Long) {
        shelfDao.clearBooksFromShelf(shelfId)
        shelfDao.deleteShelf(shelfId)
    }

    override suspend fun addBookToShelves(bookId: Long, shelfIds: List<Long>) {
        shelfIds.forEach { shelfId ->
            shelfDao.insertBookToShelf(ShelfBookCrossRef(shelfId, bookId))
        }
    }

    override suspend fun removeBookFromShelf(shelfId: Long, bookId: Long) {
        shelfDao.removeBookFromShelf(ShelfBookCrossRef(shelfId, bookId))
        val remainingCount = shelfDao.getBookCountInShelf(shelfId)
        if (remainingCount == 0) {
            shelfDao.deleteShelf(shelfId)
        }
    }

    override suspend fun updateShelfWithBooks(shelfId: Long, newName: String, orderedBookIds: List<Long>): Boolean {
        val trimmedName = newName.trim()
        val existing = shelfDao.getShelfByName(trimmedName)

        if (existing != null && existing.id != shelfId) return false
        if (orderedBookIds.isEmpty()) return false

        val currentShelf = shelfDao.getShelfByName(trimmedName) ?: ShelfEntity(id = shelfId, name = trimmedName)
        shelfDao.insertShelf(currentShelf.copy(name = trimmedName))

        shelfDao.clearBooksFromShelf(shelfId)
        orderedBookIds.forEachIndexed { index, bookId ->
            shelfDao.insertBookToShelf(ShelfBookCrossRef(shelfId = shelfId, bookId = bookId, position = index))
        }
        return true
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