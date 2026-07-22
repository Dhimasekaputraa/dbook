package com.dhimsea.dbook.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dhimsea.dbook.domain.model.BookFormat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil{
//    Copy file from URI (SAF) to app internal storage
    fun copyUriToInternalStorage(context: Context, uri: Uri): File? {
        return try {
            val fileName = getFileName(context, uri) ?: "imported_book_${System.currentTimeMillis()}"
            val booksDir = File(context.filesDir, "books").apply {
                if (!exists()) mkdirs()
            }

            val destinationFile = File(booksDir, fileName)
            val inputStream : InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destinationFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
//    Check the book format
    fun getBookFormat(fileName: String): BookFormat? {
        return when{
            fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            else -> null
        }
    }
}