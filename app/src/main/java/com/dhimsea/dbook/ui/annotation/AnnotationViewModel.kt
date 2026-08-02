package com.dhimsea.dbook.ui.annotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnnotationViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {


    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _book.value = bookRepository.getBookById(bookId)
        }
    }

    fun getAnnotationsForBook(bookId: Long): Flow<List<Annotation>> {
        return bookRepository.getAnnotationsForBook(bookId)
    }

    fun deleteAnnotation(annotation: Annotation) {
        viewModelScope.launch {
            bookRepository.deleteAnnotation(annotation)
        }
    }
}

class AnnotationViewModelFactory(
    private val bookRepository: BookRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnnotationViewModel::class.java)) {
            return AnnotationViewModel(bookRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}