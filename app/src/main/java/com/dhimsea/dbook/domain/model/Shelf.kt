package com.dhimsea.dbook.domain.model

data class Shelf(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShelfWithBooks(
    val shelf: Shelf,
    val books: List<Book>
)