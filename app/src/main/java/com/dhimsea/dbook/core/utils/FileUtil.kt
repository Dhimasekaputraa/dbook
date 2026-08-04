package com.dhimsea.dbook.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.dhimsea.dbook.domain.model.BookFormat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil{

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

    fun getBookFormat(fileName: String): BookFormat? {
        return when{
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            else -> null
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = -1L
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) size = it.getLong(index)
                }
            }
        }
        return size
    }

    fun saveCoverToInternalStorage(context: Context, bitmap: Bitmap, bookId: String): String {
        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
        val coverFile = File(coversDir, "$bookId.png")
        
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        
        return coverFile.absolutePath
    }

    fun uriToBase64(context: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            Log.d("FileUtil", "Trying to open URI: $uri")
            Log.d("FileUtil", "URI scheme: ${uri.scheme}")
            
            val inputStream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else {
                val file = File(uriString)
                if (file.exists()) file.inputStream() else null
            }
            
            Log.d("FileUtil", "InputStream: $inputStream")
            
            inputStream?.use { input ->
                val bytes = input.readBytes()
                Log.d("FileUtil", "Bytes read: ${bytes.size}")
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("FileUtil", "Exception: ${e.message}", e)
            null
        }
    }
}