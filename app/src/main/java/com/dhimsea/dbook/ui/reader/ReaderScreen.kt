package com.dhimsea.dbook.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dhimsea.dbook.core.utils.LocalBookServer
import com.dhimsea.dbook.domain.model.Annotation
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import com.dhimsea.dbook.ui.components.QuoteShareDialog
import com.dhimsea.dbook.data.repository.DictionaryRepository
import com.dhimsea.dbook.domain.model.DictionaryUiState
import com.dhimsea.dbook.ui.components.DictionaryDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import java.net.URLEncoder

@Parcelize
data class ChapterMarker(
    val label: String,
    val percent: Float,
    val pageNum: Int = 1,
    val href: String = ""
) : Parcelable

data class PendingSelection(
    val cfi: String,
    val text: String,
    val posX: Float,
    val posY: Float,
    val bottomY: Float? = null
)

class ReaderBridge(
    private val onProgressUpdate: (Int, Int, Float, String, String) -> Unit,
    private val onToggleOverview: () -> Unit,
    private val onOpenOverview: () -> Unit,
    private val onCloseOverview: () -> Unit,
    private val onChaptersLoaded: (List<ChapterMarker>) -> Unit,
    private val onTextSelected: (String, String, Float, Float, Float) -> Unit,
    private val onSelectionCleared: () -> Unit,
    private val onIndexingProgressUpdate: (Int) -> Unit,
    private val onSearchFinished: (String) -> Unit,
    private val onOpenQuoteShare: (String, String, String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun updateProgress(
        page: Int,
        total: Int,
        percent: Float,
        chapterName: String,
        cfi: String
    ) {
        mainHandler.post {
            onProgressUpdate(page, total, percent, chapterName, cfi)
        }
    }

    @JavascriptInterface
    fun toggleOverview() { mainHandler.post { onToggleOverview() } }

    @JavascriptInterface
    fun openOverview() { mainHandler.post { onOpenOverview() } }

    @JavascriptInterface
    fun closeOverview() { mainHandler.post { onCloseOverview() } }

    @JavascriptInterface
    fun onChaptersLoaded(jsonString: String) {
        mainHandler.post {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<ChapterMarker>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChapterMarker(
                            label = obj.getString("label"),
                            percent = obj.optDouble("percent", 0.0).toFloat(),
                            pageNum = obj.optInt("pageNum", 1),
                            href = obj.optString("href", "")
                        )
                    )
                }
                onChaptersLoaded(list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun onTextSelectedWithPosition(cfi: String, text: String, posX: Float, posY: Float, bottomY: Float) {
        mainHandler.post { onTextSelected(cfi, text, posX, posY, bottomY) }
    }

    @JavascriptInterface
    fun onSelectionCleared() {
        mainHandler.post { onSelectionCleared() }
    }

    @JavascriptInterface
    fun onSearchResultsLoaded(jsonResults: String) {
        mainHandler.post { onSearchFinished(jsonResults) }
    }

    @JavascriptInterface
    fun onIndexingProgress(percent: Int) {
        mainHandler.post { onIndexingProgressUpdate(percent) }
    }

    @JavascriptInterface
    fun openQuoteShareDialog(quoteText: String, title: String, author: String) {
        CoroutineScope(Dispatchers.Main).launch {
            onOpenQuoteShare(quoteText, title, author)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ReaderScreen(
    filePath: String,
    initialCfiToJump: String? = null,
    targetPercentToJump: Float? = null,
    targetHrefToJump: String? = null,
    searchQueryToHighlight: String? = null,
    bookRepository: BookRepository,
    onOpenAnnotationScreen: (Long) -> Unit,
    onOpenTocScreen: (Long, List<ChapterMarker>, String, Int) -> Unit,
    onNavigateToSearch: (Long, String, String) -> Unit,
    onBack: () -> Unit,
    onHrefJumpHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSystemDark = isSystemInDarkTheme()
    val density = LocalDensity.current.density

    var showAppearanceBottomSheet by remember { mutableStateOf(false) }
    var readerSettings by remember {
        mutableStateOf(ReaderSettings.loadFromPrefs(context, isSystemDark))
    }

    var currentBook by remember { mutableStateOf<Book?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isServerReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var isOverviewMode by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentPercent by remember { mutableFloatStateOf(0f) }
    var currentChapter by remember { mutableStateOf("Loading Chapter...") }
    var chapters by remember { mutableStateOf<List<ChapterMarker>>(emptyList()) }

    var indexingProgress by remember { mutableIntStateOf(0) }
    var isIndexing by remember { mutableStateOf(true) }

    var latestCfi by remember { mutableStateOf<String?>(null) }

    var pendingSelection by remember { mutableStateOf<PendingSelection?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }

    var pendingSearchQuery by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var isSearchBarExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showQuoteShareDialog by remember { mutableStateOf(false) }
    var quoteTextToShare by remember { mutableStateOf("") }
    var bookTitleToShare by remember { mutableStateOf("") }
    var bookAuthorToShare by remember { mutableStateOf("") }

    var showDictionaryDialog by remember { mutableStateOf(false) }
    var selectedWordForDictionary by remember { mutableStateOf("") }
    var dictionaryUiState by remember { mutableStateOf<DictionaryUiState>(DictionaryUiState.Loading) }

    val dictionaryRepository = remember { DictionaryRepository() }

    val presetColors = listOf(
        "#FFEB3B" to Color(0xFFFFEB3B),
        "#4CAF50" to Color(0xFF4CAF50),
        "#2196F3" to Color(0xFF2196F3),
        "#E91E63" to Color(0xFFE91E63)
    )

    val webView = remember(context) {
        object : WebView(context) {
            private val emptyActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    menu?.clear()
                    return true
                }
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    menu?.clear()
                    return true
                }
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
                override fun onDestroyActionMode(mode: ActionMode?) {}
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

            setBackgroundColor(if (readerSettings.isDarkMode) AndroidColor.parseColor("#121212") else AndroidColor.parseColor("#ffffff"))

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
                    latestCfi = cfi

                    Log.d("DBOOK_DEBUG", "UPDATE PROGRESS -> Page: $page | CFI Baru: $cfi")

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
                onTextSelected = { cfi, text, posX, posY, bottomY ->
                    pendingSelection = PendingSelection(cfi, text, posX, posY, bottomY)
                },
                onSelectionCleared = {
                    pendingSelection = null
                },
                onIndexingProgressUpdate = { percent ->
                    indexingProgress = percent
                    if (percent >= 100) {
                        isIndexing = false
                    }
                },
                onSearchFinished = { jsonResults ->
                    isSearching = false
                    val bookId = currentBook?.id
                    val query = pendingSearchQuery

                    if (bookId != null && query != null) {
                        onNavigateToSearch(bookId, query, jsonResults)
                    }
                    pendingSearchQuery = null
                },
                onOpenQuoteShare = { quoteText, title, author ->
                    quoteTextToShare = quoteText
                    bookTitleToShare = title.ifBlank { currentBook?.title ?: "Unknown Title" }
                    bookAuthorToShare = author.ifBlank { currentBook?.author ?: "Unknown Author" }
                    showQuoteShareDialog = true
                }
            ), "Android")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        if (!url.contains("127.0.0.1:8080")) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                Log.e("ReaderScreen", "Failed to open browser: ${e.message}")
                            }
                        }
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false

                    val initScript = "javascript:updateTextFormatting('${readerSettings.fontFamily}', ${readerSettings.fontSize}, ${readerSettings.lineHeight}, '${readerSettings.textAlign}', ${readerSettings.isDarkMode});"
                    view?.evaluateJavascript(initScript, null)

                    if (!targetHrefToJump.isNullOrEmpty()) {
                        Log.d("DBOOK_DEBUG", "WebView Selesai Load -> Eksekusi Jump Href: $targetHrefToJump")
                        val escapedHref = targetHrefToJump.replace("'", "\\'")
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            view?.evaluateJavascript("javascript:goToChapterHref('$escapedHref');") {
                                Log.d("DBOOK_DEBUG", "Navigasi Href Selesai Dieksekusi ke JS")
                                onHrefJumpHandled()
                            }
                        }, 500)
                    } 

                    else if (!initialCfiToJump.isNullOrEmpty()) {
                        val query = searchQueryToHighlight ?: ""
                        Handler(Looper.getMainLooper()).postDelayed({
                            view?.evaluateJavascript(
                                "javascript:goToSearchResult('$initialCfiToJump', '$query');",
                                null
                            )
                        }, 400)
                    }

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
        }
    }

    LaunchedEffect(webView) {
        webViewRef = webView
    }

    LaunchedEffect(isServerReady, currentBook) {
        if (isServerReady && currentBook != null && webView.url == null) {
            val targetCfi = initialCfiToJump ?: currentBook?.lastReadCfi
            val encodedCfi = if (!targetCfi.isNullOrEmpty()) URLEncoder.encode(targetCfi, "UTF-8") else ""
            val bookIdParam = currentBook?.id ?: 0L

            Log.d("DBOOK_DEBUG", "=== LOAD READER INITIAL ===")
            webView.loadUrl("http://127.0.0.1:8080/reader.html?cfi=$encodedCfi&bookId=$bookIdParam")
        }
    }

    LaunchedEffect(isOverviewMode) {
        webViewRef?.evaluateJavascript("javascript:setOverviewState($isOverviewMode);", null)
    }

    BackHandler(enabled = isOverviewMode) {
        isOverviewMode = false
        webViewRef?.evaluateJavascript("javascript:setOverviewState(false);", null)
        onBack()
    }

    LaunchedEffect(
        readerSettings.isDarkMode,
        readerSettings.fontFamily,
        readerSettings.fontSize,
        readerSettings.lineHeight,
        readerSettings.textAlign
    ) {
        webViewRef?.let { webViewInstance ->
            val script = "javascript:updateTextFormatting('${readerSettings.fontFamily}', ${readerSettings.fontSize}, ${readerSettings.lineHeight}, '${readerSettings.textAlign}', ${readerSettings.isDarkMode});"
            webViewInstance.evaluateJavascript(script, null)
        }
    }

    LaunchedEffect(initialCfiToJump) {
        if (!initialCfiToJump.isNullOrEmpty()) {
            delay(500)
            webViewRef?.evaluateJavascript("goToSearchResult('$initialCfiToJump', '$searchQueryToHighlight');", null)
        }
    }

    LaunchedEffect(targetPercentToJump) {
        targetPercentToJump?.let { percent ->
            delay(300)
            webViewRef?.evaluateJavascript("javascript:goToPercent($percent);", null)
        }
    }

    val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val animatedScale by animateFloatAsState(targetValue = if (isOverviewMode) 0.76f else 1.0f, animationSpec = spec, label = "")
    val animatedOffsetY by animateFloatAsState(targetValue = if (isOverviewMode) 6f else 0f, animationSpec = spec, label = "")
    val animatedCornerRadius by animateFloatAsState(targetValue = if (isOverviewMode) 16f else 0f, animationSpec = spec, label = "")
    val animatedElevation by animateFloatAsState(targetValue = if (isOverviewMode) 12f else 0f, animationSpec = spec, label = "")
    val outerBackgroundColor = if (isOverviewMode) MaterialTheme.colorScheme.surfaceContainer else if (readerSettings.isDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)

    val server = remember { LocalBookServer(context, 8080) }

    LaunchedEffect(filePath) {
        scope.launch(Dispatchers.IO) { currentBook = bookRepository.getBookByFilePath(filePath) }
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

    val window = (context as? Activity)?.window
    LaunchedEffect(isOverviewMode) {
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isOverviewMode) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("DBOOK_DEBUG", "=== EXIT READER ===")
            if (server.isAlive) server.stop()

            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }

            currentBook?.let { book ->
                val cfiToSave = latestCfi ?: book.lastReadCfi
                scope.launch(Dispatchers.IO) {
                    bookRepository.updateReadingProgress(
                        bookId = book.id,
                        page = currentPage,
                        cfi = cfiToSave,
                        progress = currentPercent
                    )
                }
            }
        }
    }

    fun executeSearch(query: String) {
        if (query.isBlank()) return
        keyboardController?.hide()
        pendingSearchQuery = query
        isSearching = true

        val escaped = query.replace("\\", "\\\\").replace("'", "\\'")
        webViewRef?.evaluateJavascript("searchInBook('$escaped');", null)
    }

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

    fun onTriggerDefine(word: String) {
        selectedWordForDictionary = word
        showDictionaryDialog = true
        dictionaryUiState = DictionaryUiState.Loading

        scope.launch {
            dictionaryUiState = dictionaryRepository.getDefinition(word)
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(outerBackgroundColor)
    ) {
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
                if (isSearchBarExpanded) {
                    IconButton(onClick = {
                        isSearchBarExpanded = false
                        searchText = ""
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search in book...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch(searchText) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(onClick = { executeSearch(searchText) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currentBook?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { isSearchBarExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search in book",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showAppearanceBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Reader Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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
                    factory = { webView }
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

            AnimatedVisibility(
                visible = isLoading || isIndexing,
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            ) {
                IndexingLoadingScreen(
                    bookTitle = currentBook?.title ?: "",
                    isIndexing = isIndexing,
                    progress = indexingProgress,
                    backgroundColor = outerBackgroundColor
                )
            }
        }

        if (pendingSelection != null && !showNoteDialog) {
            val screenWidthPx = (LocalConfiguration.current.screenWidthDp * density).toInt()
            val screenHeightPx = (LocalConfiguration.current.screenHeightDp * density).toInt()

            val pxX = (pendingSelection!!.posX * density).toInt()
            val pxTopY = (pendingSelection!!.posY * density).toInt()
            val pxBottomY = ((pendingSelection!!.bottomY ?: pendingSelection!!.posY) * density).toInt()

            val popupWidthPx = (220 * density).toInt()
            val popupHeightPx = (210 * density).toInt()

            val selectionHeightPx = pxBottomY - pxTopY

            val isFullPageSelection = selectionHeightPx > (screenHeightPx * 0.50f) || 
                                     (pxTopY < (100 * density) && pxBottomY > screenHeightPx - (120 * density))

            val targetX: Int
            val targetY: Int

            if (isFullPageSelection) {
                targetX = (screenWidthPx - popupWidthPx) / 2
                targetY = (screenHeightPx - popupHeightPx) / 2
            } else {
                val topGapOffsetPx = (52 * density).toInt()
                val bottomGapOffsetPx = (24 * density).toInt()

                targetX = (pxX - (16 * density).toInt()).coerceIn(16, screenWidthPx - popupWidthPx - 16)

                val hasSpaceOnTop = pxTopY - popupHeightPx - topGapOffsetPx > 0

                targetY = if (hasSpaceOnTop) {
                    pxTopY - popupHeightPx - topGapOffsetPx
                } else {                    
                    (pxBottomY + bottomGapOffsetPx).coerceAtMost(screenHeightPx - popupHeightPx - 16)
                }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = targetX, y = targetY),
                onDismissRequest = {
                    pendingSelection = null
                },
                properties = PopupProperties(
                    focusable = false
                )
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .padding(vertical = 8.dp)
                    ) {
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
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val selectedText = pendingSelection?.text ?: ""
                                    pendingSelection = null
                                    webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)

                                    if (selectedText.isNotEmpty()) {
                         
                                        onTriggerDefine(selectedText)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Define",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Define",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingSelection = null
                                    webViewRef?.evaluateJavascript("triggerShareAsImage();", null)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share as Image",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Share as Image",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

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
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            currentBook?.id?.let { id -> 
                                onOpenTocScreen(id, chapters, currentChapter, currentPage)
                            }
                        }
                        .padding(bottom = 6.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
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

    if (showNoteDialog && pendingSelection != null) {
        var noteInput by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#FFEB3B") }

        AlertDialog(
            onDismissRequest = {
                showNoteDialog = false
                pendingSelection = null
                webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)
            },
            title = { Text("Add note") },
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
                        label = { Text("Write a note here") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Column {
                        Text(
                            text = "Select highlight color:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { (hex, color) ->
                                val isSelected = selectedColorHex == hex

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
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
                                            selectedColorHex = hex
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { saveAnnotation(selectedColorHex, noteInput) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNoteDialog = false
                    pendingSelection = null
                    webViewRef?.evaluateJavascript("window.getSelection().removeAllRanges();", null)
                }) { Text("Cancel") }
            }
        )
    }

    if (showDictionaryDialog) {
        DictionaryDialog(
            selectedWord = selectedWordForDictionary,
            uiState = dictionaryUiState,
            onDismissRequest = {
                showDictionaryDialog = false
            }
        )
    }

    if (showQuoteShareDialog) {
        QuoteShareDialog(
            quoteText = quoteTextToShare,
            bookTitle = bookTitleToShare,
            bookAuthor = bookAuthorToShare,
            onDismiss = { showQuoteShareDialog = false }
        )
    }
    if (showAppearanceBottomSheet) {
        ReaderAppearanceBottomSheet(
            settings = readerSettings,
            onSettingsChanged = { updatedSettings ->
                readerSettings = updatedSettings
                ReaderSettings.saveToPrefs(context, updatedSettings)
            },
            onDismissRequest = {
                showAppearanceBottomSheet = false
            }
        )
    }
}

@Composable
fun IndexingLoadingScreen(
    bookTitle: String,
    isIndexing: Boolean,
    progress: Int,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (bookTitle.isNotBlank()) {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isIndexing && progress > 0) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Preparing your Book... $progress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Just a moment...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}