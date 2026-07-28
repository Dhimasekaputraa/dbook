package com.dhimsea.dbook.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dhimsea.dbook.core.utils.LocalBookServer
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URLEncoder

data class ChapterMarker(val label: String, val percent: Float)

data class PendingSelection(
    val cfi: String,
    val text: String,
    val posX: Float,
    val posY: Float
)

class ReaderBridge(
    private val onProgressUpdate: (Int, Int, Float, String, String) -> Unit,
    private val onToggleOverview: () -> Unit,
    private val onOpenOverview: () -> Unit,
    private val onCloseOverview: () -> Unit,
    private val onChaptersLoaded: (List<ChapterMarker>) -> Unit,
    private val onTextSelected: (String, String, Float, Float) -> Unit,
    private val onSelectionCleared: () -> Unit
) {
    @JavascriptInterface
    fun updateProgress(currentPage: Int, totalPages: Int, percent: Float, chapterName: String, cfi: String) {
        onProgressUpdate(currentPage, totalPages, percent, chapterName, cfi)
    }

    @JavascriptInterface
    fun toggleOverview() { onToggleOverview() }

    @JavascriptInterface
    fun openOverview() { onOpenOverview() }

    @JavascriptInterface
    fun closeOverview() { onCloseOverview() }

    @JavascriptInterface
    fun onChaptersLoaded(jsonString: String) {
        try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<ChapterMarker>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChapterMarker(
                        label = obj.getString("label"),
                        percent = obj.getDouble("percent").toFloat()
                    )
                )
            }
            onChaptersLoaded(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onTextSelectedWithPosition(cfi: String, text: String, posX: Float, posY: Float) {
        onTextSelected(cfi, text, posX, posY)
    }

    @JavascriptInterface
    fun onSelectionCleared() {
        onSelectionCleared()
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ReaderScreen(
    filePath: String,
    initialCfiToJump: String? = null,
    bookRepository: BookRepository,
    onOpenAnnotationScreen: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkMode = isSystemInDarkTheme()
    val density = LocalDensity.current.density

    var currentBook by remember { mutableStateOf<Book?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isServerReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var isOverviewMode by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentPercent by remember { mutableFloatStateOf(0f) }
    var currentChapter by remember { mutableStateOf("Memuat Chapter...") }
    var chapters by remember { mutableStateOf<List<ChapterMarker>>(emptyList()) }

    // State Seleksi Teks & Dialog Note
    var pendingSelection by remember { mutableStateOf<PendingSelection?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#FFEB3B" to Color(0xFFFFEB3B), // Yellow
        "#4CAF50" to Color(0xFF4CAF50), // Green
        "#2196F3" to Color(0xFF2196F3), // Blue
        "#E91E63" to Color(0xFFE91E63)  // Pink
    )

    LaunchedEffect(isOverviewMode) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        if (isOverviewMode) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            webViewRef?.evaluateJavascript("setOverviewState(true);", null)
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            webViewRef?.evaluateJavascript("setOverviewState(false);", null)
        }
    }

    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val animatedScale by animateFloatAsState(targetValue = if (isOverviewMode) 0.76f else 1.0f, animationSpec = spec, label = "")
    val animatedOffsetY by animateFloatAsState(targetValue = if (isOverviewMode) -20f else 0f, animationSpec = spec, label = "")
    val animatedCornerRadius by animateFloatAsState(targetValue = if (isOverviewMode) 16f else 0f, animationSpec = spec, label = "")
    val animatedElevation by animateFloatAsState(targetValue = if (isOverviewMode) 12f else 0f, animationSpec = spec, label = "")
    val outerBackgroundColor = if (isOverviewMode) MaterialTheme.colorScheme.surfaceContainer else if (isDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)

    val server = remember { LocalBookServer(context, 8080) }

    LaunchedEffect(filePath) {
        scope.launch(Dispatchers.IO) { currentBook = bookRepository.getBookByFilePath(filePath) }
    }

    LaunchedEffect(isDarkMode) {
        webViewRef?.evaluateJavascript("setTheme($isDarkMode);", null)
    }

    LaunchedEffect(filePath) {
        try {
            server.serveBook(filePath)
            if (!server.isAlive) server.start()
            delay(100)
            isServerReady = true
        } catch (e: Exception) {
            Log.e("ReaderScreen", "Failed starting LocalBookServer: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (server.isAlive) server.stop()
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Fungsi Simpan Anotasi ke Database & WebView
    fun saveAnnotation(colorHex: String, noteText: String = "") {
        val currentSelection = pendingSelection ?: return
        val bookId = currentBook?.id ?: return

        scope.launch(Dispatchers.IO) {
            val newAnnotation = Annotation(
                bookId = bookId,
                cfi = currentSelection.cfi,
                chapterName = currentChapter,
                pageNumber = currentPage,
                text = currentSelection.text,
                note = noteText,
                colorHex = colorHex
            )
            bookRepository.insertAnnotation(newAnnotation)
        }

        webViewRef?.evaluateJavascript("applyHighlight('${currentSelection.cfi}', '$colorHex');", null)
        pendingSelection = null
        showNoteDialog = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(outerBackgroundColor)
    ) {
        // --- OVERVIEW TOP BAR ---
        AnimatedVisibility(
            visible = isOverviewMode,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = currentBook?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // --- WEBVIEW CONTAINER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationY = animatedOffsetY * density
                }
                .shadow(elevation = animatedElevation.dp, shape = RoundedCornerShape(animatedCornerRadius.dp))
                .clip(RoundedCornerShape(animatedCornerRadius.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isServerReady) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        // OVERRIDE ACTION MODE TAPI TETAP KEMBALIKAN TRUE
                        object : WebView(ctx) {
                            private val emptyActionModeCallback = object : ActionMode.Callback {
                                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                    // Bersihkan menu Copy/Share bawaan Android secara paksa
                                    menu?.clear()
                                    // KEMBALIKAN TRUE: Android akan tetap menandai blok teks (kursor biru),
                                    // dan tidak merusak gesture sistem!
                                    return true 
                                }
                                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                    menu?.clear()
                                    return true
                                }
                                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
                                override fun onDestroyActionMode(mode: ActionMode?) {
                                    // Sinyal bahwa teks di-unselect (misal tap layar sembarangan)
                                }
                            }

                            override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
                                return super.startActionMode(emptyActionModeCallback)
                            }

                            override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                                return super.startActionMode(emptyActionModeCallback, type)
                            }

                        }.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            setBackgroundColor(if (isDarkMode) AndroidColor.parseColor("#121212") else AndroidColor.parseColor("#ffffff"))

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = true
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }

                            addJavascriptInterface(ReaderBridge(
                                onProgressUpdate = { page, total, percent, chapter, cfi ->
                                    currentPage = page
                                    totalPages = total
                                    currentPercent = percent
                                    currentChapter = chapter

                                    currentBook?.let { book ->
                                        scope.launch(Dispatchers.IO) {
                                            bookRepository.updateReadingProgress(bookId = book.id, page = page, cfi = cfi, progress = percent)
                                        }
                                    }
                                },
                                onToggleOverview = { isOverviewMode = !isOverviewMode },
                                onOpenOverview = { isOverviewMode = true },
                                onCloseOverview = { isOverviewMode = false },
                                onChaptersLoaded = { chapterList -> chapters = chapterList },
                                onTextSelected = { cfi, text, posX, posY ->
                                    // Teks diblok -> Tampilkan Popup Custom
                                    pendingSelection = PendingSelection(cfi, text, posX, posY)
                                },
                                onSelectionCleared = {
                                    // Klik di tempat lain -> Hapus Popup Custom
                                    pendingSelection = null
                                }
                            ), "Android")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    view?.evaluateJavascript("setTheme($isDarkMode);", null)
                                    
                                    // Load Anotasi yang sudah disimpan sebelumnya
                                    currentBook?.id?.let { bookId ->
                                        scope.launch(Dispatchers.IO) {
                                            bookRepository.getAnnotationsForBook(bookId).collect { annotations ->
                                                val jsonArray = JSONArray()
                                                annotations.forEach { item ->
                                                    val obj = org.json.JSONObject().apply {
                                                        put("id", item.id)
                                                        put("cfi", item.cfi)
                                                        put("colorHex", item.colorHex)
                                                    }
                                                    jsonArray.put(obj)
                                                }
                                                launch(Dispatchers.Main) {
                                                    view?.evaluateJavascript("loadAnnotations('${jsonArray.toString()}');", null)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val targetCfi = initialCfiToJump ?: currentBook?.lastReadCfi
                            val encodedCfi = if (!targetCfi.isNullOrEmpty()) URLEncoder.encode(targetCfi, "UTF-8") else ""
                            loadUrl("http://127.0.0.1:8080/reader.html?cfi=$encodedCfi")
                            webViewRef = this
                        }
                    }
                )
            }

            if (isOverviewMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isOverviewMode = false
                        }
                )
            }

            if (isLoading) {
                CircularProgressIndicator()
            }
        }

        // --- POPUP MENU CUSTOM (MUNCUL DI LOKASI KLIK/PILIH SEPERTI DI LIBRARY) ---
        if (pendingSelection != null && !showNoteDialog) {
            val pxX = (pendingSelection!!.posX * density).toInt()
            val pxY = (pendingSelection!!.posY * density).toInt()

            Popup(
                alignment = Alignment.TopStart,
                // Kita posisikan sedikit di atas/bawah area sentuh agar tidak tertutup jari
                offset = IntOffset(x = (pxX - (100 * density)).toInt(), y = (pxY + (10 * density)).toInt()), 
                onDismissRequest = {
                    pendingSelection = null
                    webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)
                },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        // BARIS 1: BULATAN WARNA UNTUK HIGHLIGHT LANGSUNG
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { (hex, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { saveAnnotation(hex, "") }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        // BARIS 2: MENU ADD NOTE SEPERTI LIBRARY SCREEN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNoteDialog = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Add Note",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Add Note",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // --- BOTTOM DOCK SLIDER ---
        AnimatedVisibility(
            visible = isOverviewMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = currentChapter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentBook?.id?.let { id -> onOpenAnnotationScreen(id) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Annotation", tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                        ) {
                            val totalWidth = maxWidth
                            chapters.forEach { chapter ->
                                val dotOffset = totalWidth * chapter.percent
                                Box(modifier = Modifier.offset(x = dotOffset).size(5.dp).background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape = CircleShape))
                            }
                        }

                        Slider(
                            value = currentPercent,
                            onValueChange = { currentPercent = it },
                            onValueChangeFinished = { webViewRef?.evaluateJavascript("goToPercent($currentPercent);", null) },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(text = "$currentPage/$totalPages", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // --- MODAL DIALOG UNTUK MENULIS NOTE ---
    if (showNoteDialog && pendingSelection != null) {
        var noteInput by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#FFEB3B") }

        AlertDialog(
            onDismissRequest = {
                showNoteDialog = false
                pendingSelection = null
                webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)
            },
            title = { Text("Tambah Catatan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "“${pendingSelection?.text}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Tuliskan Catatan...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Column {
                        Text(
                            text = "Pilih Warna Highlight:", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // Disesuaikan agar spacing presisi seperti di Popup
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { (hex, color) ->
                                val isSelected = selectedColorHex == hex
                                
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        // Indikator warna terpilih berupa outline/border ramping tanpa efek shadow yang bikin kedip
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            } else Modifier
                                        )
                                        .clickable { 
                                            // Hanya mengubah state lokal warna, TIDAK memanggil saveAnnotation() langsung
                                            selectedColorHex = hex 
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { saveAnnotation(selectedColorHex, noteInput) }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNoteDialog = false
                    pendingSelection = null
                    webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)
                }) { Text("Batal") }
            }
        )
    }
}