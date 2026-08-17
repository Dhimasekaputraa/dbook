package com.dhimsea.dbook.ui.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.model.ShelfWithBooks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShelfScreen(
    shelfWithBooks: ShelfWithBooks,
    allBooks: List<Book>,
    onBackClick: () -> Unit,
    onSaveShelf: (shelfId: Long, name: String, bookIds: List<Long>, onResult: (Boolean, String?) -> Unit) -> Unit
) {
    var shelfName by remember { mutableStateOf(shelfWithBooks.shelf.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddBooksDialog by remember { mutableStateOf(false) }

    val currentBooks = remember { mutableStateListOf<Book>().apply { addAll(shelfWithBooks.books) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Shelf", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (shelfName.trim().isEmpty()) {
                                errorMessage = "Shelf name cannot be empty."
                                return@TextButton
                            }

                            onSaveShelf(
                                shelfWithBooks.shelf.id,
                                shelfName,
                                currentBooks.map { it.id }
                            ) { success, errorMsg ->
                                if (success) {
                                    onBackClick()
                                } else {
                                    errorMessage = errorMsg
                                }
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = shelfName,
                onValueChange = {
                    shelfName = it
                    errorMessage = null
                },
                label = { Text("Shelf Name") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Books in Shelf (${currentBooks.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { showAddBooksDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Book")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(currentBooks, key = { _, book -> "shelf_book_${book.id}" }) { index, book ->
                    DraggableBookItem(
                        book = book,
                        index = index,
                        totalItems = currentBooks.size,
                        onMove = { fromIndex, toIndex ->
                            if (fromIndex != toIndex && fromIndex in 0 until currentBooks.size && toIndex in 0 until currentBooks.size) {
                                val item = currentBooks.removeAt(fromIndex)
                                currentBooks.add(toIndex, item)
                            }
                        },
                        onRemove = { currentBooks.remove(book) }
                    )
                }
            }
        }
    }

    if (showAddBooksDialog) {
        AddBooksToShelfDialog(
            allBooks = allBooks,
            currentShelfBookIds = currentBooks.map { it.id }.toSet(),
            onDismiss = { showAddBooksDialog = false },
            onBookAdded = { book ->
                if (currentBooks.none { it.id == book.id }) {
                    currentBooks.add(book)
                }
            }
        )
    }
}

@Composable
fun DraggableBookItem(
    book: Book,
    index: Int,
    totalItems: Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        label = "item_elevation"
    )

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) offsetY else 0f
            }
            .shadow(elevation, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDragging) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(index, totalItems) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                offsetY = 0f
                            },
                            onDragEnd = {
                                isDragging = false
                                val deltaIndex = Math.round(offsetY / itemHeightPx)
                                val targetIndex = (index + deltaIndex).coerceIn(0, totalItems - 1)
                                if (targetIndex != index) {
                                    onMove(index, targetIndex)
                                }
                                offsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                            }
                        )
                    }
            )

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
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove Book",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AddBooksToShelfDialog(
    allBooks: List<Book>,
    currentShelfBookIds: Set<Long>,
    onDismiss: () -> Unit,
    onBookAdded: (Book) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val availableBooks = remember(searchQuery, allBooks, currentShelfBookIds) {
        val unadded = allBooks.filter { it.id !in currentShelfBookIds }
        if (searchQuery.isBlank()) unadded
        else unadded.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Books to Shelf",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title or author...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No books match search." else "All books have been added.",
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
                        items(availableBooks, key = { "dialog_book_${it.id}" }) { book ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = book.title,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = book.author,
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
                                                    imageVector = Icons.Default.Book,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                },

                                trailingContent = {
                                    IconButton(
                                        onClick = { onBookAdded(book) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Add Book",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onBookAdded(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}