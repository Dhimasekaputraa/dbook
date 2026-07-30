package com.dhimsea.dbook.domain.model

data class SearchResult(
    val cfi: String,
    val excerpt: String,
    val chapter: String,
    val page: Int
)