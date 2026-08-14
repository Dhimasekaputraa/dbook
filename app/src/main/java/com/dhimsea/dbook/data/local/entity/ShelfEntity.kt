package com.dhimsea.dbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dhimsea.dbook.domain.model.Shelf

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shelf_book_cross_ref",
    primaryKeys = ["shelfId", "bookId"],
    indices = [Index(value = ["bookId"])],
    foreignKeys = [
        ForeignKey(
            entity = ShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShelfBookCrossRef(
    val shelfId: Long,
    val bookId: Long,
    val position: Int = 0
)

fun ShelfEntity.toDomainModel(): Shelf = Shelf(id = id, name = name, createdAt = createdAt)
fun Shelf.toEntity(): ShelfEntity = ShelfEntity(id = id, name = name, createdAt = createdAt)