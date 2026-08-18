package com.dhimsea.dbook.ui.library

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Book as BookIcon
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.ShelfWithBooks
import kotlinx.coroutines.flow.collectLatest

enum class SortOption(val displayName: String) {
    DATE_ADDED("Date Added"),
    NAME("Name"),
    LAST_READ("Last Read")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (Book) -> Unit,
    onAnnotationClick: (Book) -> Unit,
    onNavigateToEditShelf: (Long) -> Unit
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val shelvesWithBooks by viewModel.shelvesWithBooks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isGridView by viewModel.isGridView.collectAsState()
    val selectedSortOption by viewModel.selectedSortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }

    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var bookToFinish by remember { mutableStateOf<Book?>(null) }
    var bookToAddToShelf by remember { mutableStateOf<Book?>(null) }

    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    
    var shelfToDelete by remember { mutableStateOf<ShelfWithBooks?>(null) }
    var bookToRemoveFromShelf by remember { mutableStateOf<Pair<ShelfWithBooks, Book>?>(null) }

    val continueReadingBooks = remember(books) {
        books.filter { book -> book.progressPercentage > 0f }
            .sortedByDescending { it.lastReadAt }
    }

    val processedBooks = remember(searchQuery, books, selectedSortOption, isAscending) {
        var result = if (searchQuery.isBlank()) {
            books
        } else {
            books.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true)
            }
        }

        result = when (selectedSortOption) {
            SortOption.DATE_ADDED -> if (isAscending) result.sortedBy { it.addedAt } else result.sortedByDescending { it.addedAt }
            SortOption.NAME -> if (isAscending) result.sortedBy { it.title.lowercase() } else result.sortedByDescending { it.title.lowercase() }
            SortOption.LAST_READ -> if (isAscending) result.sortedBy { it.lastReadAt } else result.sortedByDescending { it.lastReadAt }
        }

        result
    }

    val filteredBooksForSearch = remember(searchQuery, books) {
        if (searchQuery.isBlank()) emptyList()
        else books.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importBooks(uris)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isSystemInDark = isSystemInDarkTheme()
    val view = LocalView.current

    if (!view.isInEditMode) {
        DisposableEffect(isSystemInDark) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.isAppearanceLightStatusBars = !isSystemInDark
            }

            onDispose {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isSystemInDark
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { isSearchActive = false },
                            active = isSearchActive,
                            onActiveChange = { isSearchActive = it },
                            placeholder = { Text("Search Book Title or Author...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = SearchBarDefaults.colors(
                                containerColor = if (isSearchActive) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when {
                                searchQuery.isBlank() -> { }
                                filteredBooksForSearch.isEmpty() -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Book not found.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        items(filteredBooksForSearch, key = { it.id }) { book ->
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = book.title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                supportingContent = {
                                                    Text(
                                                        text = book.author,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                leadingContent = {
                                                    if (!book.coverPath.isNullOrEmpty()) {
                                                        AsyncImage(
                                                            model = book.coverPath,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .size(width = 36.dp, height = 48.dp)
                                                                .clip(MaterialTheme.shapes.extraSmall)
                                                        )
                                                    } else {
                                                        Surface(
                                                            modifier = Modifier.size(width = 36.dp, height = 48.dp),
                                                            shape = MaterialTheme.shapes.extraSmall,
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.BookIcon,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(
                                                    containerColor = Color.Transparent
                                                ),
                                                modifier = Modifier.clickable {
                                                    isSearchActive = false
                                                    onBookClick(book)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isSearchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    TextButton(onClick = { showSortDropdown = true }) {
                                        Text(
                                            text = selectedSortOption.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showSortDropdown,
                                        onDismissRequest = { showSortDropdown = false }
                                    ) {
                                        SortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        RadioButton(
                                                            selected = (selectedSortOption == option),
                                                            onClick = null
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(option.displayName)
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setSortOption(option)
                                                    showSortDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.toggleSortDirection() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Sort Direction",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.toggleGridView() }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List View"
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSearchActive) {
                    LibraryFabMenu(
                        isExpanded = isFabExpanded,
                        onToggleFab = { isFabExpanded = !isFabExpanded },
                        onAddBook = {
                            isFabExpanded = false
                            multiFilePickerLauncher.launch(arrayOf("application/epub+zip"))
                        },
                        onCreateShelf = {
                            isFabExpanded = false
                            showCreateShelfDialog = true
                        }
                    )
                }
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isScanning,
                onRefresh = { viewModel.refreshLibrary() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (processedBooks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "No Books Match \"$searchQuery\""
                            else
                                "No books have been added yet.\nPress + to add books.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (searchQuery.isBlank() && continueReadingBooks.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "Continue Reading",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(continueReadingBooks, key = { "continue_${it.id}" }) { book ->
                                            LargeContinueReadingCard(
                                                book = book,
                                                onClick = { onBookClick(book) },
                                                onAddToShelf = { bookToAddToShelf = book },
                                                onDelete = { bookToDelete = book },
                                                onAnnotations = { onAnnotationClick(book) },
                                                onToggleFinish = { bookToFinish = book }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }

                        if (searchQuery.isBlank() && shelvesWithBooks.isNotEmpty()) {
                            items(shelvesWithBooks, key = { "shelf_${it.shelf.id}" }) { shelfWithBooks ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                
                                    ShelfHeader(
                                        shelfName = shelfWithBooks.shelf.name,
                                        onEditShelfClick = { 
                                            onNavigateToEditShelf(shelfWithBooks.shelf.id)
                                        },
                                        onDeleteShelfClick = { shelfToDelete = shelfWithBooks }
                                    )

                                    if (shelfWithBooks.books.isEmpty()) {
                                        Text(
                                            text = "This shelf is empty. Tap the three dots to edit and add books.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    } else {
                                        LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(shelfWithBooks.books, key = { "shelf_${shelfWithBooks.shelf.id}_${it.id}" }) { book ->
                                            ShelfBookCard(
                                                book = book,
                                                onClick = { onBookClick(book) },
                                                onRemoveFromShelf = {
                                                    bookToRemoveFromShelf = Pair(shelfWithBooks, book)
                                                },
                                                onAddToShelf = { bookToAddToShelf = book },
                                                onDelete = { bookToDelete = book },
                                                onAnnotations = { onAnnotationClick(book) },
                                                onToggleFinish = { bookToFinish = book }
                                            )
                                        }
                                    }
                                }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }

                        item {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Search Results" else "All Books",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (isGridView) {
                            item {
                                val gridHeight = calculateGridHeight(processedBooks.size)
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 120.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(gridHeight),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(processedBooks, key = { it.id }) { book ->
                                        BookItemCard(
                                            book = book,
                                            onClick = { onBookClick(book) },
                                            onAddToShelf = { bookToAddToShelf = book },
                                            onDelete = { bookToDelete = book },
                                            onAnnotations = { onAnnotationClick(book) },
                                            onToggleFinish = { bookToFinish = book }
                                        )
                                    }
                                }
                            }
                        } else {
                            items(processedBooks, key = { it.id }) { book ->
                                BookItemListRow(
                                    book = book,
                                    onClick = { onBookClick(book) },
                                    onAddToShelf = { bookToAddToShelf = book },
                                    onDelete = { bookToDelete = book },
                                    onAnnotations = { onAnnotationClick(book) },
                                    onToggleFinish = { bookToFinish = book }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateShelfDialog) {
        CreateShelfDialog(
            books = books,
            existingShelves = shelvesWithBooks,
            initialSelectedBookIds = bookToAddToShelf?.let { listOf(it.id) } ?: emptyList(),
            onDismiss = { 
                showCreateShelfDialog = false
                bookToAddToShelf = null
            },
            onCreateShelf = { name, bookIds, onResult ->
                viewModel.createShelf(name, bookIds) { success, errorMsg ->
                    if (success) {
                        showCreateShelfDialog = false
                        bookToAddToShelf = null
                    }
                    onResult(success, errorMsg)
                }
            }
        )
    }

    bookToAddToShelf?.let { book ->
        AddToShelfDialog(
            book = book,
            shelves = shelvesWithBooks,
            onDismiss = { bookToAddToShelf = null },
            onSaveToShelves = { selectedShelfIds ->
                viewModel.addBookToShelves(book.id, selectedShelfIds)
                bookToAddToShelf = null
            },
            onCreateShelfClick = {
                showCreateShelfDialog = true
            }
        )
    }

    bookToRemoveFromShelf?.let { (shelfWithBooks, book) ->
        AlertDialog(
            onDismissRequest = { bookToRemoveFromShelf = null },
            title = { Text("Remove from Shelf?") },
            text = {
                Text("Remove \"${book.title}\" from \"${shelfWithBooks.shelf.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookFromShelf(shelfWithBooks.shelf.id, book.id)
                        bookToRemoveFromShelf = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRemoveFromShelf = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    bookToFinish?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToFinish = null },
            title = { Text("Mark as Finished?") },
            text = {
                Text("Mark \"${book.title}\" as finished? This action will reset you current progress")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markBookAsFinished(book)
                        bookToFinish = null
                    }
                ) {
                    Text("Mark as finished")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToFinish = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book?") },
            text = {
                Text("Are you sure you want to delete \"${book.title}\"? This action will permanently remove the book and all its data.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook(book)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    shelfToDelete?.let { shelfWithBooks ->
        AlertDialog(
            onDismissRequest = { shelfToDelete = null },
            title = { Text("Delete Shelf?") },
            text = {
                Text("Are you sure you want to delete \"${shelfWithBooks.shelf.name}\"? The books inside will remain in your library.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteShelf(shelfWithBooks.shelf.id)
                        shelfToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { shelfToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateShelfDialog(
    books: List<Book>,
    existingShelves: List<ShelfWithBooks>,
    initialSelectedBookIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onCreateShelf: (String, List<Long>, (Boolean, String?) -> Unit) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var shelfName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedBookIds by remember(initialSelectedBookIds) { 
        mutableStateOf(initialSelectedBookIds.toSet()) 
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else books.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }

    if (step == 1) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Create Shelf",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a name for your new shelf.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = shelfName,
                        onValueChange = {
                            shelfName = it
                            errorMessage = null
                        },
                        label = { Text("Shelf Name") },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = shelfName.trim()
                        
                        if (trimmedName.isEmpty()) {
                            errorMessage = "Shelf name cannot be empty."
                        } else if (existingShelves.any { it.shelf.name.equals(trimmedName, ignoreCase = true) }) {
                            errorMessage = "A shelf with this name already exists."
                        } else {
                            errorMessage = null
                            step = 2
                        }
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column {
                    Text(
                        text = "Add Books to Shelf",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shelf: ${shelfName.trim()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search books...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (filteredBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "No books in library" else "No matching books",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                val isSelected = selectedBookIds.contains(book.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedBookIds = if (isSelected) {
                                                selectedBookIds - book.id
                                            } else {
                                                selectedBookIds + book.id
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedBookIds = if (checked == true) {
                                                selectedBookIds + book.id
                                            } else {
                                                selectedBookIds - book.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateShelf(
                            shelfName.trim(),
                            selectedBookIds.toList()
                        ) { success, errorMsg ->
                            if (success) {
                                onDismiss()
                            } else {
                                errorMessage = errorMsg
                            }
                        }
                    }
                ) {
                    Text("Finish")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        step = 1
                    }
                ) {
                    Text("Prev")
                }
            }
        )
    }
}

@Composable
private fun AddToShelfDialog(
    book: Book,
    shelves: List<ShelfWithBooks>,
    onDismiss: () -> Unit,
    onSaveToShelves: (List<Long>) -> Unit,
    onCreateShelfClick: () -> Unit
) {
    val existingShelfIds = remember(book.id, shelves) {
        shelves.filter { shelfWithBooks ->
            shelfWithBooks.books.any { it.id == book.id }
        }.map { it.shelf.id }.toSet()
    }

    var newlySelectedShelfIds by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Shelf",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select shelves for \"${book.title}\":",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onCreateShelfClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Create New Shelf",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    if (shelves.isEmpty()) {
                        item {
                            Text(
                                text = "No shelf available yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            )
                        }
                    } else {
                        items(shelves, key = { it.shelf.id }) { shelfWithBooks ->
                            val isAlreadyInShelf = existingShelfIds.contains(shelfWithBooks.shelf.id)
                            val isNewlyChecked = newlySelectedShelfIds.contains(shelfWithBooks.shelf.id)
                            val isChecked = isAlreadyInShelf || isNewlyChecked

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isAlreadyInShelf) {
                                        newlySelectedShelfIds = if (isNewlyChecked) {
                                            newlySelectedShelfIds - shelfWithBooks.shelf.id
                                        } else {
                                            newlySelectedShelfIds + shelfWithBooks.shelf.id
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    enabled = !isAlreadyInShelf,
                                    onCheckedChange = { checked ->
                                        if (!isAlreadyInShelf) {
                                            newlySelectedShelfIds = if (checked) {
                                                newlySelectedShelfIds + shelfWithBooks.shelf.id
                                            } else {
                                                newlySelectedShelfIds - shelfWithBooks.shelf.id
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = shelfWithBooks.shelf.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAlreadyInShelf) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = if (isAlreadyInShelf) "Already in this shelf" else "${shelfWithBooks.books.size} books",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAlreadyInShelf) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newlySelectedShelfIds.isNotEmpty()) {
                        onSaveToShelves(newlySelectedShelfIds.toList())
                    }
                    onDismiss()
                },
                enabled = newlySelectedShelfIds.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfBookCard(
    book: Book,
    onClick: () -> Unit,
    onRemoveFromShelf: () -> Unit,
    onAddToShelf: () -> Unit,
    onDelete: () -> Unit,
    onAnnotations: () -> Unit,
    onToggleFinish: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(0.7f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (!book.coverPath.isNullOrEmpty()) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = "cover ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookIcon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Open") },
                onClick = {
                    showMenu = false
                    onClick()
                },
                leadingIcon = { Icon(Icons.Default.BookIcon, contentDescription = null) }
            )

            DropdownMenuItem(
                text = { Text("Add to Shelf") },
                onClick = {
                    showMenu = false
                    onAddToShelf()
                },
                leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null) }
            )

            DropdownMenuItem(
                text = { Text("Remove from Shelf") },
                onClick = {
                    showMenu = false
                    onRemoveFromShelf()
                },
                leadingIcon = { Icon(Icons.Default.IndeterminateCheckBox, contentDescription = null) }
            )

            if (book.progressPercentage > 0f || book.lastReadPage > 0) {
                DropdownMenuItem(
                    text = { Text("Mark as Finished") },
                    onClick = {
                        showMenu = false
                        onToggleFinish()
                    },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
            }

            DropdownMenuItem(
                text = { Text("My Annotations") },
                onClick = {
                    showMenu = false
                    onAnnotations()
                },
                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
fun LargeContinueReadingCard(
    book: Book,
    onClick: () -> Unit,
    onAddToShelf: () -> Unit,
    onDelete: () -> Unit,
    onAnnotations: () -> Unit,
    onToggleFinish: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val progressPercent = (book.progressPercentage * 100).toInt()

    Column(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (!book.coverPath.isNullOrEmpty()) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookIcon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$progressPercent% completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BookDropdownMenu(
                    showMenu = showMenu,
                    onDismiss = { showMenu = false },
                    onClick = onClick,
                    onAddToShelf = onAddToShelf,
                    onAnnotations = onAnnotations,
                    onDelete = onDelete,
                    onToggleFinish = onToggleFinish,
                    showMarkAsFinished = book.progressPercentage > 0f || book.lastReadPage > 0
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItemCard(
    book: Book,
    onClick: () -> Unit,
    onAddToShelf: () -> Unit,
    onDelete: () -> Unit,
    onAnnotations: () -> Unit,
    onToggleFinish: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (!book.coverPath.isNullOrEmpty()) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = "cover ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookIcon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        BookDropdownMenu(
            showMenu = showMenu,
            onDismiss = { showMenu = false },
            onClick = onClick,
            onAddToShelf = onAddToShelf,
            onAnnotations = onAnnotations,
            onDelete = onDelete,
            onToggleFinish = onToggleFinish,
            showMarkAsFinished = book.progressPercentage > 0f || book.lastReadPage > 0
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItemListRow(
    book: Book,
    onClick: () -> Unit,
    onAddToShelf: () -> Unit,
    onDelete: () -> Unit,
    onAnnotations: () -> Unit,
    onToggleFinish: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                if (!book.coverPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = book.coverPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 40.dp, height = 56.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(width = 40.dp, height = 56.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BookIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        )

        BookDropdownMenu(
            showMenu = showMenu,
            onDismiss = { showMenu = false },
            onClick = onClick,
            onAddToShelf = onAddToShelf,
            onAnnotations = onAnnotations,
            onDelete = onDelete,
            onToggleFinish = onToggleFinish,
            showMarkAsFinished = book.progressPercentage > 0f || book.lastReadPage > 0
        )
    }
}

@Composable
fun BookDropdownMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onAddToShelf: () -> Unit,
    onAnnotations: () -> Unit,
    onDelete: () -> Unit,
    onToggleFinish: () -> Unit,
    showMarkAsFinished: Boolean
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Open") },
            onClick = {
                onDismiss()
                onClick()
            },
            leadingIcon = { Icon(Icons.Default.BookIcon, contentDescription = null) }
        )

        DropdownMenuItem(
            text = { Text("Add to Shelf") },
            onClick = {
                onDismiss()
                onAddToShelf()
            },
            leadingIcon = { Icon(Icons.Default.LibraryAdd, contentDescription = null) }
        )

        if (showMarkAsFinished) {
            DropdownMenuItem(
                text = { Text("Mark as Finished") },
                onClick = {
                    onDismiss()
                    onToggleFinish()
                },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
            )
        }

        DropdownMenuItem(
            text = { Text("My Annotations") },
            onClick = {
                onDismiss()
                onAnnotations()
            },
            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
fun ShelfHeader(
    shelfName: String,
    onEditShelfClick: () -> Unit,
    onDeleteShelfClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = shelfName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Shelf options"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Shelf") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onEditShelfClick()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete Shelf") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        onDeleteShelfClick()
                    }
                )
            }
        }
    }
}

@Composable
fun LibraryFabMenu(
    isExpanded: Boolean,
    onToggleFab: () -> Unit,
    onAddBook: () -> Unit,
    onCreateShelf: () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f, label = "fabRotation")

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onCreateShelf,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                        Text("Create Shelf", style = MaterialTheme.typography.labelLarge)
                    }
                }

                SmallFloatingActionButton(
                    onClick = onAddBook,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Book", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onToggleFab,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.height(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotationAngle)
            )
        }
    }
}

@Composable
private fun calculateGridHeight(itemCount: Int): androidx.compose.ui.unit.Dp {
    val rows = (itemCount + 1) / 2
    return (rows * 220).dp
}