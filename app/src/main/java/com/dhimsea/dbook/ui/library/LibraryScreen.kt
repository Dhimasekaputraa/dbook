package com.dhimsea.dbook.ui.library

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Book as BookIcon
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.dhimsea.dbook.domain.model.Book
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
    onAnnotationClick: (Book) -> Unit
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
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
                    FloatingActionButton(
                        onClick = { multiFilePickerLauncher.launch(arrayOf("application/epub+zip")) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Books")
                    }
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
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(continueReadingBooks, key = { "continue_${it.id}" }) { book ->
                                            LargeContinueReadingCard(
                                                book = book,
                                                onClick = { onBookClick(book) },
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
}

@Composable
fun LargeContinueReadingCard(
    book: Book,
    onClick: () -> Unit,
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
private fun calculateGridHeight(itemCount: Int): androidx.compose.ui.unit.Dp {
    val rows = (itemCount + 1) / 2
    return (rows * 220).dp
}