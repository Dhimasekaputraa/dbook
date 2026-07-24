package com.dhimsea.dbook.core.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class ScannedBook(
    val uri: Uri,
    val fileName: String,
    val displayName: String
)

object DirectoryScanner {

    fun scan(context: Context, directoryUri: Uri): List<ScannedBook> {
        val results = mutableListOf<ScannedBook>()
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return results
        scanRecursive(directory, results)
        return results
    }

    private fun scanRecursive(
        directory: DocumentFile,
        results: MutableList<ScannedBook>
    ) {
        directory.listFiles().forEach { file ->
            when {
                file.isDirectory -> scanRecursive(file, results)
                file.isFile && file.name?.endsWith(".epub", ignoreCase = true) == true -> {
                    results.add(
                        ScannedBook(
                            uri = file.uri,
                            fileName = file.name ?: "Unknown",
                            displayName = file.name?.substringBeforeLast(".") ?: "Unknown"
                        )
                    )
                }
            }
        }
    }
}