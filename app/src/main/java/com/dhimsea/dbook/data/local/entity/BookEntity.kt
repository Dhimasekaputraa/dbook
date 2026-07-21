package com.dhimsea.dbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.BookFormat

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String?,
    val format: String,
    val totalPages: Int,
    val lastReadPage: Int,
    val lastReadCfi: String?,
    val progressPercentage: Float,
    val addedAt: Long,
    val lastReadAt: Long
)

// Extension Mapper: database entity to domain model
fun BookEntity.toDomainModel(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        filePath = filePath,
        coverPath = coverPath,
        format = BookFormat.valueOf(format),
        totalPages = totalPages ,
        lastReadPage = lastReadPage,
        lastReadCfi = lastReadCfi,
        progressPercentage = progressPercentage,
        addedAt = addedAt,
        lastReadAt = lastReadAt
    )
}

// Extension Mapper: domain model to database entity
fun Book.toEntity(): BookEntity{
    return BookEntity(
        id = id,
        title = title,
        author = author,
        filePath = filePath,
        coverPath = coverPath,
        format = format.name,
        totalPages = totalPages ,
        lastReadPage = lastReadPage,
        lastReadCfi = lastReadCfi,
        progressPercentage = progressPercentage,
        addedAt = addedAt,
        lastReadAt = lastReadAt
    )
}