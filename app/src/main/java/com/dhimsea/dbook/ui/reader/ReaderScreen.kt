package com.dhimsea.dbook.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint
@Composable
fun ReaderScreen(
    fileUri: String,
    onBack: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                webViewClient = WebViewClient()

                loadUrl("file:///android_asset/reader.html")
            }
        },
        update = { webView ->
            // TODO (Tahap 3): Di sinilah nanti kita akan mengubah fileUri (path buku)
            // menjadi data byte/Base64 dan menyuntikkannya ke fungsi JS loadBookData()
        }
    )
}