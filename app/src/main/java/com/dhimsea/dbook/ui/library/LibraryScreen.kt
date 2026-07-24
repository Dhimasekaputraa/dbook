package com.dhimsea.dbook.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhimsea.dbook.domain.model.Book
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val books by viewModel.books.collectAsState()
    val activeDirectory by viewModel.activeDirectory.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColor.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val displayName = it.lastPathSegment ?: "Selected Folder"
            viewModel.setWatchedDirectory(it, displayName)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importSingleBook(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet() {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Library Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Watched folder section
                Text(
                    text = "LIBRARY FOLDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                ListItem(
                    headlineContent = {
                        Text(
                            text = activeDirectory?.displayName ?: "No folder selected",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Text(if (activeDirectory != null) "Tap to change folder" else "Tap to select folder")
                    },
                    leadingContent = {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    },
                    // Mengubah warna background menjadi transparan
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { folderPickerLauncher.launch(null) }
                        )
                        .padding(horizontal = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Appearance section
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                ListItem(
                    headlineContent = { Text("Dark Mode") },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) }
                        )
                    },
                    // Mengubah warna background menjadi transparan
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
                ListItem(
                    headlineContent = { Text("Dynamic Color") },
                    trailingContent = {
                        Switch(
                            checked = isDynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) }
                        )
                    },
                    // Mengubah warna background menjadi transparan
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Your Library") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book")
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
                if (books.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeDirectory == null)
                                "No folder selected.\nOpen menu to select a folder."
                            else
                                "No books found.\nPull down to refresh or tap + to add books.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(books, key = { it.id }) { book ->
                            BookCard(
                                book = book,
                                onClick = {
                                    // TODO: Navigate to reader
                                },
                                onDelete = { viewModel.deleteBook(book) },
                                onAnnotations = {
                                    // TODO: Navigate to annotations
                                },
                                onDetail = {
                                    // TODO: Navigate to book detail
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAnnotations: () -> Unit,
    onDetail: () -> Unit
) {
    // State menu dipindah ke dalam komponen card
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Placeholder cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
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

        // Material 3 Dropdown Menu
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
                leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("My Annotations") },
                onClick = {
                    showMenu = false
                    onAnnotations()
                },
                leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("View Book Detail") },
                onClick = {
                    showMenu = false
                    onDetail()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
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