package com.dhimsea.dbook.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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
                          
                            name.endsWith(".opf") -> {
                                opfContent = zip.readBytes().toString(Charsets.UTF_8)
                            }
                          
                            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") -> {
                                imageMap[name] = zip.readBytes()
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

        if (opfContent != null) {
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(opfContent!!.reader())

                var insideMetadata = false
                var coverItemId: String? = null
                val itemMapById = mutableMapOf<String, String>()
                val allImageHrefs = mutableListOf<String>()
                var epub3CoverHref: String? = null
                var guideCoverHref: String? = null

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name.lowercase()
                            when (tagName) {
                                "metadata" -> insideMetadata = true

                                "dc:title", "title" -> {
                                    if (insideMetadata && title == null) {
                                        title = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                    }
                                }

                                "dc:creator", "creator" -> {
                                    if (insideMetadata && author == null) {
                                        author = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                    }
                                }

                                "meta" -> {
                                    val metaName = parser.getAttributeValue(null, "name")
                                    val metaContent = parser.getAttributeValue(null, "content")
                                    if (metaName.equals("cover", ignoreCase = true) && metaContent != null) {
                                        coverItemId = metaContent
                                    }
                                }

                                "item" -> {
                                    val itemId = parser.getAttributeValue(null, "id")
                                    val itemHref = parser.getAttributeValue(null, "href")
                                    val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                                    val properties = parser.getAttributeValue(null, "properties") ?: ""

                                    if (itemId != null && itemHref != null) {
                                        itemMapById[itemId] = itemHref
                                    }

                                    if (mediaType.startsWith("image/") && itemHref != null) {
                                        allImageHrefs.add(itemHref)

                                        if (properties.contains("cover-image")) {
                                            epub3CoverHref = itemHref
                                        }
                                    }
                                }

                                "reference" -> {
                                    val type = parser.getAttributeValue(null, "type")
                                    val href = parser.getAttributeValue(null, "href")
                                    if (type.equals("cover", ignoreCase = true) && href != null) {
                                        guideCoverHref = href
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

                var targetHref: String? = epub3CoverHref

                if (targetHref == null && coverItemId != null) {
                    targetHref = itemMapById[coverItemId]
                }

                if (targetHref == null && guideCoverHref != null) {
                    val cleanGuide = guideCoverHref.substringBefore("#").substringAfterLast("/")
                    targetHref = allImageHrefs.find { it.contains(cleanGuide.substringBefore("."), ignoreCase = true) }
                }

                if (targetHref == null) {
                    val keywords = listOf("cvi", "cover", "front", "title")
                    for (keyword in keywords) {
                        val matched = allImageHrefs.find { it.substringAfterLast("/").lowercase().contains(keyword) }
                        if (matched != null) {
                            targetHref = matched
                            break
                        }
                    }
                }

                if (targetHref == null) {
                    targetHref = allImageHrefs.firstOrNull()
                }

                if (targetHref != null) {
                    val decodedHref = try {
                        URLDecoder.decode(targetHref, StandardCharsets.UTF_8.name()).lowercase()
                    } catch (e: Exception) {
                        targetHref.lowercase()
                    }
                    val fileName = decodedHref.substringAfterLast("/")

                    val matchKey = imageMap.keys.firstOrNull { key ->
                        key.endsWith(fileName) || key.endsWith(decodedHref)
                    }
                    coverBytes = matchKey?.let { imageMap[it] }
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