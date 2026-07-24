package com.dhimsea.dbook.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class EpubMetadata(
    val title: String?,
    val author: String?,
    val coverBitmap: Bitmap?
)

object EpubUtils {

    fun extractMetadata(context: Context, uri: Uri): EpubMetadata {
        var title: String? = null
        var author: String? = null
        var coverBytes: ByteArray? = null
        var opfContent: String? = null
        val imageMap = mutableMapOf<String, ByteArray>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        when {
                            // Baca file OPF untuk metadata title & author
                            name.endsWith(".opf") -> {
                                opfContent = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            // Simpan semua image untuk dicari cover-nya nanti
                            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") -> {
                                imageMap[entry.name] = zip.readBytes()
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse OPF untuk title, author, dan cover image id
        if (opfContent != null) {
            try {
                var coverImageHref: String? = null

                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(opfContent!!.reader())

                var insideMetadata = false
                var coverItemId: String? = null

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name.lowercase()) {
                                "metadata" -> insideMetadata = true

                                // Ambil title
                                "dc:title", "title" -> {
                                    if (insideMetadata && title == null) {
                                        title = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                    }
                                }

                                // Ambil author
                                "dc:creator", "creator" -> {
                                    if (insideMetadata && author == null) {
                                        author = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                    }
                                }

                                // Ambil cover image id dari meta tag
                                "meta" -> {
                                    val metaName = parser.getAttributeValue(null, "name")
                                    val metaContent = parser.getAttributeValue(null, "content")
                                    if (metaName == "cover" && metaContent != null) {
                                        coverItemId = metaContent
                                    }
                                }

                                // Cari href dari item yang id-nya cocok dengan coverItemId
                                "item" -> {
                                    val itemId = parser.getAttributeValue(null, "id")
                                    val itemHref = parser.getAttributeValue(null, "href")
                                    val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                                    if (itemId != null && itemId == coverItemId && itemHref != null) {
                                        coverImageHref = itemHref
                                    }
                                    // Fallback: item yang media-type image dan id mengandung "cover"
                                    if (coverImageHref == null &&
                                        mediaType.startsWith("image/") &&
                                        itemId?.lowercase()?.contains("cover") == true
                                    ) {
                                        coverImageHref = itemHref
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name.lowercase() == "metadata") insideMetadata = false
                        }
                    }
                    eventType = parser.next()
                }

                // Cari cover bytes dari imageMap berdasarkan href
                if (coverImageHref != null) {
                    val matchKey = imageMap.keys.firstOrNull { it.endsWith(coverImageHref!!) }
                    coverBytes = matchKey?.let { imageMap[it] }
                }

                // Fallback: cari gambar yang namanya mengandung "cover"
                if (coverBytes == null) {
                    val fallbackKey = imageMap.keys.firstOrNull {
                        it.lowercase().contains("cover")
                    }
                    coverBytes = fallbackKey?.let { imageMap[it] }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val bitmap = coverBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        return EpubMetadata(
            title = title,
            author = author,
            coverBitmap = bitmap
        )
    }
}