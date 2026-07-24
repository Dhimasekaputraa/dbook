package com.dhimsea.dbook.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.core.utils.DirectoryScanner
import com.dhimsea.dbook.data.local.entity.ScanDirectoryEntity
import com.dhimsea.dbook.core.utils.FileUtil
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.BookFormat
import com.dhimsea.dbook.domain.repository.BookRepository
import com.dhimsea.dbook.domain.repository.ScanDirectoryRepository
import com.dhimsea.dbook.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel (
    private val bookRepository: BookRepository,
    private val scanRepository: ScanDirectoryRepository,
    private val themeRepository: ThemeRepository,
    private val context: Context 
):  ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDirectory: StateFlow<ScanDirectoryEntity?> = scanRepository
        .getActiveDirectory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDynamicColor: StateFlow<Boolean> = themeRepository.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setDarkMode(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setDynamicColor(enabled) }
    }

    // Tambah satu file epub via tombol +
    fun importSingleBook(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileSize = FileUtil.getFileSize(context, uri)

                // cek duplikat by file size
                if (fileSize != -1L && bookRepository.isFileSizeExists(fileSize)) return@launch

                val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return@launch
                val fileName = documentFile.name ?: return@launch
                if (!fileName.endsWith(".epub", ignoreCase = true)) return@launch

                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                bookRepository.insertBook(
                    Book(
                        title = fileName.substringBeforeLast("."),
                        filePath = uri.toString(),
                        format = BookFormat.EPUB,
                        fileSize = fileSize
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteBook(book: Book) {
    viewModelScope.launch(Dispatchers.IO) {
        bookRepository.deleteBook(book)
    }
}

    // Set watched folder baru + sync
    fun setWatchedDirectory(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val currentPaths = bookRepository.getAllFilePaths()
                val activeDir = activeDirectory.value
                if (activeDir != null) {
                    val oldDirUri = Uri.parse(activeDir.uriString)
                    currentPaths.forEach { path ->
                        if (path.startsWith(oldDirUri.toString())) {
                            bookRepository.deleteBookByFilePath(path)
                        }
                    }
                }
                scanRepository.setDirectory(uri, displayName)
                syncWithDirectory(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Refresh manual — sync buku dengan watched folder
    fun refreshLibrary() {
        val dir = activeDirectory.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            syncWithDirectory(Uri.parse(dir.uriString))
        }
    }

    private suspend fun syncWithDirectory(directoryUri: Uri) {
        _isScanning.value = true
        try {
            val scannedBooks = DirectoryScanner.scan(context, directoryUri)
            val existingPaths = bookRepository.getAllFilePaths().toSet()

            scannedBooks.forEach { scanned ->
                val filePath = scanned.uri.toString()
                if (filePath !in existingPaths) {
                    // cek duplikat by file size
                    val fileSize = FileUtil.getFileSize(context, scanned.uri)
                    if (fileSize != -1L && bookRepository.isFileSizeExists(fileSize)) return@forEach

                    bookRepository.insertBook(
                        Book(
                            title = scanned.displayName,
                            filePath = filePath,
                            format = BookFormat.EPUB,
                            fileSize = fileSize
                        )
                    )
                }
            }

            val scannedPaths = scannedBooks.map { it.uri.toString() }.toSet()
            existingPaths.forEach { path ->
                if (path.startsWith(directoryUri.toString()) && path !in scannedPaths) {
                    bookRepository.deleteBookByFilePath(path)
                }
            }
        } finally {
            _isScanning.value = false
        }
    }
}

class LibraryViewModelFactory(
    private val bookRepository: BookRepository,
    private val scanRepository: ScanDirectoryRepository,
    private val themeRepository: ThemeRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(bookRepository, scanRepository, themeRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}