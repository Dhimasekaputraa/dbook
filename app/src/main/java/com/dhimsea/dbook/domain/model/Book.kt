package com.dhimsea.dbook.domain.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "Unknown",
    val filePath: String,
    val coverPath: String? = null,
    val format: BookFormat,
    val fileSize: Long = -1L,
    val totalPages: Int = 0,
    val lastReadPage: Int = 0,
    val lastReadCfi: String? = null,
    val progressPercentage: Float = 0f,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)
