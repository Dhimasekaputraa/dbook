package com.dhimsea.dbook.domain.model

data class Annotation(
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