package com.dhimsea.dbook.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class LocalBookServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    private var bookPath: String? = null

    fun serveBook(filePath: String) {
        bookPath = filePath
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d("LocalBookServer", "Request URI: $uri")

        // 1. SERVE FILE EPUB BUKU
        if (uri == "/book.epub") {
            val path = bookPath ?: return notFoundResponse()
            
            // Karena LibraryViewModel selalu menyimpan ke internal storage (absolutePath),
            // kita bisa langsung membacanya sebagai File biasa.
            val file = File(path)
            if (file.exists()) {
                try {
                    val inputStream = FileInputStream(file)
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        "application/epub+zip",
                        inputStream,
                        file.length() // Sangat krusial agar JSZip mengetahui ukuran asli file
                    ).apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                        addHeader("Accept-Ranges", "bytes") // Membantu epub.js melakukan partial load
                    }
                } catch (e: Exception) {
                    Log.e("LocalBookServer", "Error opening book file: ${e.message}")
                }
            }
            return notFoundResponse()
        }

        // 2. SERVE ASSETS (js, css, reader.html)
        return try {
            val assetPath = uri.trimStart('/')
            val inputStream = context.assets.open(assetPath)
            val mimeType = when {
                uri.endsWith(".js") -> "application/javascript"
                uri.endsWith(".css") -> "text/css"
                uri.endsWith(".html") -> "text/html"
                else -> "application/octet-stream"
            }
            newChunkedResponse(Response.Status.OK, mimeType, inputStream).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Exception) {
            notFoundResponse()
        }
    }

    private fun notFoundResponse() = newFixedLengthResponse(
        Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found"
    )
}