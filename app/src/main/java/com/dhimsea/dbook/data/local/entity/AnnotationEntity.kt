package com.dhimsea.dbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index 

@Entity(tableName = "annotations", foreignKeys = [
    ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )
], indices = [Index("bookId")]
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val cfi: String,
    val chapterName: String,
    val pageNumber: Int,
    val text: String,
    val note: String = "",
    val colorHex: String = "#FFEB3B",
    val createdAt: Long = System.currentTimeMillis()
)