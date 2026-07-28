package com.dhimsea.dbook.data.local.dao

import androidx.room.*
import com.dhimsea.dbook.data.local.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getAnnotationsForBook(bookId: Long): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)
}