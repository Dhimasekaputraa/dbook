package com.dhimsea.dbook.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.util.Log
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    private val onOpenOverview: () -> Unit,
    private val onCloseOverview: () -> Unit,
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
    fun openOverview() {
        onOpenOverview()
    }

    @JavascriptInterface
    fun closeOverview() {
        onCloseOverview()
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

    // Efek Status Bar & Kunci Scroll WebView via JS
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

    // ANIMASI GPU TRANSISI LELAP
    val spec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isOverviewMode) 0.76f else 1.0f,
        animationSpec = spec,
        label = "animatedScale"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isOverviewMode) -20f else 0f,
        animationSpec = spec,
        label = "animatedOffsetY"
    )

    val animatedCornerRadius by animateFloatAsState(
        targetValue = if (isOverviewMode) 16f else 0f,
        animationSpec = spec,
        label = "animatedCornerRadius"
    )

    val animatedElevation by animateFloatAsState(
        targetValue = if (isOverviewMode) 12f else 0f,
        animationSpec = spec,
        label = "animatedElevation"
    )

    val outerBackgroundColor = if (isOverviewMode) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        if (isDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)
    }

    val server = remember { LocalBookServer(context, 8080) }

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
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(outerBackgroundColor)
    ) {
        // --- TOP BAR OVERVIEW ---
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
                .shadow(
                    elevation = animatedElevation.dp,
                    shape = RoundedCornerShape(animatedCornerRadius.dp)
                )
                .clip(RoundedCornerShape(animatedCornerRadius.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isServerReady) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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
                                    isOverviewMode = !isOverviewMode
                                },
                                onOpenOverview = {
                                    isOverviewMode = true
                                },
                                onCloseOverview = {
                                    isOverviewMode = false
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

            // TRANSPARENT CLICK OVERLAY SAAT OVERVIEW MODE
            // Mencegah interaksi scroll teks & Memicu Kembali ke Fullscreen jika ditap
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                        ) {
                            val totalWidth = maxWidth
                            chapters.forEach { chapter ->
                                val dotOffset = totalWidth * chapter.percent
                                Box(
                                    modifier = Modifier
                                        .offset(x = dotOffset)
                                        .size(5.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$currentPage/$totalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}