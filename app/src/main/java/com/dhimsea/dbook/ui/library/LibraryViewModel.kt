package com.dhimsea.dbook.ui.library

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.core.utils.EpubUtils
import com.dhimsea.dbook.core.utils.FileUtil
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.BookFormat
import com.dhimsea.dbook.domain.repository.BookRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("library_settings", Context.MODE_PRIVATE)

    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _selectedSortOption = MutableStateFlow(
        SortOption.valueOf(prefs.getString("sort_option", SortOption.DATE_ADDED.name) ?: SortOption.DATE_ADDED.name)
    )
    val selectedSortOption: StateFlow<SortOption> = _selectedSortOption

    private val _isAscending = MutableStateFlow(prefs.getBoolean("is_ascending", false))
    val isAscending: StateFlow<Boolean> = _isAscending

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "import_progress_channel"
    private val NOTIF_ID = 1001

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Import Book Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Display progress for importing book"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun importBooks(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            var successCount = 0
            var failCount = 0
            val total = uris.size

            val canShowNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Importing Books")
                .setOngoing(true)
                .setOnlyAlertOnce(true)

            uris.forEachIndexed { index, uri ->
                val current = index + 1
                if (canShowNotification) {
                    notifBuilder.setContentText("Processing $current from $total books")
                    notifBuilder.setProgress(total, current, false)
                    try {
                        notificationManager.notify(NOTIF_ID, notifBuilder.build())
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }

                val success = processSingleUri(uri)
                if (success) successCount++ else failCount++
            }

            if (canShowNotification) {
                notifBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle("Import Finished")
                    .setContentText("Success: $successCount, Failed: $failCount")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                try {
                    notificationManager.notify(NOTIF_ID, notifBuilder.build())
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }

            _isScanning.value = false

            val resultMessage = when {
                successCount > 0 && failCount == 0 -> "Successfully imported $successCount books."
                successCount > 0 && failCount > 0 -> "$successCount books successfully imported, $failCount failed (duplicate)."
                else -> "Import Failed. No new books were added."
            }
            _uiMessage.emit(resultMessage)
        }
    }

    fun toggleGridView() {
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        prefs.edit().putBoolean("is_grid_view", newValue).apply()
    }

    fun setSortOption(option: SortOption) {
        _selectedSortOption.value = option
        prefs.edit().putString("sort_option", option.name).apply()
    }

    fun toggleSortDirection() {
        val newValue = !_isAscending.value
        _isAscending.value = newValue
        prefs.edit().putBoolean("is_ascending", newValue).apply()
    }

    private suspend fun processSingleUri(uri: Uri): Boolean {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return false
            val fileName = documentFile.name ?: return false
            if (!fileName.endsWith(".epub", ignoreCase = true)) return false

            val fileSize = FileUtil.getFileSize(context, uri)
            val metadata = EpubUtils.extractMetadata(context, uri)
            val bookTitle = metadata.title?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast(".")
            
            if (fileSize > 0 && bookRepository.isBookExists(bookTitle, fileSize)) {
                return false
            }

            val copiedFile = FileUtil.copyUriToInternalStorage(context, uri) ?: return false
            val internalPath = copiedFile.absolutePath

            val coverPath = metadata.coverBitmap?.let { bitmap ->
                FileUtil.saveCoverToInternalStorage(
                    context = context,
                    bitmap = bitmap,
                    bookId = UUID.randomUUID().toString()
                )
            }

            bookRepository.insertBook(
                Book(
                    title = metadata.title?.takeIf { it.isNotBlank() }
                        ?: fileName.substringBeforeLast("."),
                    author = metadata.author?.takeIf { it.isNotBlank() }
                        ?: "Unknown Author",
                    filePath = internalPath,
                    coverPath = coverPath,
                    format = BookFormat.EPUB,
                    fileSize = fileSize
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- MARK AS FINISHED ---
    fun markBookAsFinished(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val resetBook = book.copy(
                progressPercentage = 0f,
                lastReadPage = 0,
                lastReadCfi = null
            )
            bookRepository.updateBook(resetBook)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.deleteBook(book)
            if (!book.coverPath.isNullOrEmpty()) {
                File(book.coverPath).delete()
            }
            File(book.filePath).delete()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val existingPaths = bookRepository.getAllFilePaths()
                existingPaths.forEach { path ->
                    if (!File(path).exists()) {
                        bookRepository.deleteBookByFilePath(path)
                    }
                }
                delay(600)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }
}

class LibraryViewModelFactory(
    private val bookRepository: BookRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(bookRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}