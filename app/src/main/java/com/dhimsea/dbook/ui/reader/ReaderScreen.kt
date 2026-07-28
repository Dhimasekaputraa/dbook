package com.dhimsea.dbook.ui.reader

import android.annotation.SuppressLint
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dhimsea.dbook.core.utils.LocalBookServer
import com.dhimsea.dbook.domain.model.Book
import com.dhimsea.dbook.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URLEncoder

data class ChapterMarker(val label: String, val percent: Float)

class ReaderBridge(
    private val onProgressUpdate: (Int, Int, Float, String, String) -> Unit,
    private val onToggleOverview: () -> Unit,
    private val onChaptersLoaded: (List<ChapterMarker>) -> Unit
) {
    @JavascriptInterface
    fun updateProgress(currentPage: Int, totalPages: Int, percent: Float, chapterName: String, cfi: String) {
        onProgressUpdate(currentPage, totalPages, percent, chapterName, cfi)
    }

    @JavascriptInterface
    fun toggleOverview() {
        onToggleOverview()
    }

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
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ReaderScreen(
    filePath: String,
    bookRepository: BookRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isDarkMode = isSystemInDarkTheme()

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val webViewScale by animateFloatAsState(
        targetValue = if (isOverviewMode) 0.82f else 1f,
        animationSpec = tween(300),
        label = "WebViewScale"
    )

    val server = remember { LocalBookServer(context, 8080) }

    // Fetch book data from database to get lastReadCfi
    LaunchedEffect(filePath) {
        scope.launch(Dispatchers.IO) {
            currentBook = bookRepository.getBookByFilePath(filePath)
        }
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
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isServerReady) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(webViewScale)
                    .clip(RoundedCornerShape(if (isOverviewMode) 20.dp else 0.dp)),
                factory = { ctx ->
                    WebView(ctx).apply {
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

                                // Save progress to database automatically
                                currentBook?.let { book ->
                                    scope.launch(Dispatchers.IO) {
                                        bookRepository.updateReadingProgress(
                                            bookId = book.id,
                                            page = page,
                                            cfi = cfi,
                                            progress = percent
                                        )
                                    }
                                }
                            },
                            onToggleOverview = {
                                isOverviewMode = true
                            },
                            onChaptersLoaded = { chapterList ->
                                chapters = chapterList
                            }
                        ), "Android")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.evaluateJavascript("setTheme($isDarkMode);", null)
                            }
                        }

                        // Pass initial CFI to URL
                        val initialCfi = currentBook?.lastReadCfi
                        val encodedCfi = if (!initialCfi.isNullOrEmpty()) {
                            URLEncoder.encode(initialCfi, "UTF-8")
                        } else ""

                        loadUrl("http://127.0.0.1:8080/reader.html?cfi=$encodedCfi")
                        webViewRef = this
                    }
                }
            )
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        // --- MATERIAL 3 MODAL BOTTOM SHEET ---
        if (isOverviewMode) {
            ModalBottomSheet(
                onDismissRequest = { isOverviewMode = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                scrimColor = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentChapter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Halaman $currentPage dari $totalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // SLIDER DENGAN INDIKATOR CHAPTER DOTS
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val totalWidth = maxWidth
                            chapters.forEach { chapter ->
                                val dotOffset = totalWidth * chapter.percent
                                Box(
                                    modifier = Modifier
                                        .offset(x = dotOffset)
                                        .size(6.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Slider(
                            value = currentPercent,
                            onValueChange = { currentPercent = it },
                            onValueChangeFinished = {
                                webViewRef?.evaluateJavascript("goToPercent($currentPercent);", null)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}