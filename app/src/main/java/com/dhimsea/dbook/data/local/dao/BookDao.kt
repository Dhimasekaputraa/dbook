package com.dhimsea.dbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhimsea.dbook.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id ")
    suspend fun getBooksById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity): Unit

    @Delete
    suspend fun  deleteBook(book: BookEntity): Unit

    @Query("UPDATE books SET lastReadPage = :page, lastReadCfi = :cfi, progressPercentage = :progress, lastReadAt = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, cfi:String?, progress: Float, timestamp: Long = System.currentTimeMillis()): Unit
}