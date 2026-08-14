package com.dhimsea.dbook.data.local.dao

import androidx.room.*
import com.dhimsea.dbook.data.local.entity.BookEntity
import com.dhimsea.dbook.data.local.entity.ShelfEntity
import com.dhimsea.dbook.data.local.entity.ShelfBookCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Query("SELECT * FROM shelves WHERE name = :name LIMIT 1")
    suspend fun getShelfByName(name: String): ShelfEntity?

    @Query("SELECT * FROM shelves ORDER BY createdAt DESC")
    fun getAllShelves(): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookToShelf(crossRef: ShelfBookCrossRef)

    @Delete
    suspend fun removeBookFromShelf(crossRef: ShelfBookCrossRef)

    @Query("DELETE FROM shelf_book_cross_ref WHERE shelfId = :shelfId")
    suspend fun clearBooksFromShelf(shelfId: Long)

    @Query("DELETE FROM shelves WHERE id = :shelfId")
    suspend fun deleteShelf(shelfId: Long)

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN shelf_book_cross_ref ref ON b.id = ref.bookId 
        WHERE ref.shelfId = :shelfId
        ORDER BY ref.position ASC
    """)
    fun getBooksForShelf(shelfId: Long): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM shelf_book_cross_ref WHERE shelfId = :shelfId")
    suspend fun getBookCountInShelf(shelfId: Long): Int

    @Query("DELETE FROM shelf_book_cross_ref WHERE bookId = :bookId")
    suspend fun deleteBookFromAllShelves(bookId: Long)
}