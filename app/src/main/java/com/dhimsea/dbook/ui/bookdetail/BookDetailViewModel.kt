package com.dhimsea.dbook.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val bookId: Long
) : ViewModel() {

    val book: StateFlow<Book?> = bookRepository.observeBookById(bookId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Tetap aktif selama UI terbuka
            initialValue = null
        )
}

class BookDetailViewModelFactory(
    private val bookRepository: BookRepository,
    private val bookId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookDetailViewModel(bookRepository, bookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}