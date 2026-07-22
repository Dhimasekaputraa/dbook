package com.dhimsea.dbook.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.core.utils.FileUtil
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel (
    private val repository: BookRepository
): ViewModel(){
//    change flow from Room to stateflow for implementation with compose ui
    val books: StateFlow<List<Book>> = repository.getAllBooks()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
//    Processing file that have been selected by user from storage access framework
    fun importBook(context: Context, uri: Uri){
        viewModelScope.launch(Dispatchers.IO){
            try{
//                get the file name
                val originalFileName = FileUtil.getFileName(context, uri) ?: return@launch
//                get the book format
                val format = FileUtil.getBookFormat(originalFileName) ?: return@launch
//                copy file to internal app storage
                val copiedFile = FileUtil.copyUriToInternalStorage(context, uri)

                if (copiedFile != null){
//                    get the book title only
                    val title = originalFileName.substringBeforeLast(".")
//                    create book object
                    val newBook = Book(
                        title = title,
                        filePath = copiedFile.absolutePath,
                        format = format
                    )
                    repository.insertBook(newBook)
                }
            } catch (e: Exception){
                e.printStackTrace()
//                TODO: add MutableSharedFlow to trigger SnackBar error
            }
        }
    }
}

class LibraryViewModelFactory(
    private val repository: BookRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}