package com.dhimsea.dbook.data.repository

import com.dhimsea.dbook.domain.model.DefinitionItem
import com.dhimsea.dbook.domain.model.DefinitionResult
import com.dhimsea.dbook.domain.model.DictionaryResponse
import com.dhimsea.dbook.domain.model.DictionaryUiState
import com.dhimsea.dbook.domain.model.MeaningGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DictionaryRepository(
    private val cacheMap: MutableMap<String, DefinitionResult> = mutableMapOf()
) {
    suspend fun getDefinition(word: String): DictionaryUiState = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
            .replace(Regex("[^a-zA-Z\\s-]"), "")

        if (cleanWord.isBlank()) return@withContext DictionaryUiState.Error("Invalid word")

        if (cacheMap.containsKey(cleanWord)) {
            return@withContext DictionaryUiState.Success(cacheMap[cleanWord]!!)
        }

        try {
            val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$cleanWord")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "dbook-android-app/1.0.1")
            }

            val responseCode = connection.responseCode

            when (responseCode) {
                200 -> {
                    val reader = InputStreamReader(connection.inputStream)
                    val responseType = object : TypeToken<List<DictionaryResponse>>() {}.type
                    val apiResponses: List<DictionaryResponse>? = Gson().fromJson(reader, responseType)
                    reader.close()

                    val firstEntry = apiResponses?.firstOrNull()
                    if (firstEntry != null) {
                        val phoneticText = firstEntry.phonetic 
                            ?: firstEntry.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text

                        val rawMeanings = firstEntry.meanings ?: emptyList()

                        val meaningGroups: List<MeaningGroup> = rawMeanings.map { meaning ->
                            val defItems: List<DefinitionItem> = meaning.definitions.map { def ->
                                DefinitionItem(
                                    definition = def.definition,
                                    example = def.example
                                )
                            }

                            val combinedSynonyms = mutableListOf<String>()
                            meaning.synonyms?.let { combinedSynonyms.addAll(it) }
                            meaning.definitions.forEach { def ->
                                def.synonyms?.let { combinedSynonyms.addAll(it) }
                            }

                            MeaningGroup(
                                partOfSpeech = meaning.partOfSpeech,
                                definitions = defItems,
                                synonyms = combinedSynonyms.distinct()
                            )
                        }

                        if (meaningGroups.isEmpty()) {
                            return@withContext DictionaryUiState.NotFound
                        }

                        val result = DefinitionResult(
                            word = firstEntry.word,
                            phonetic = phoneticText,
                            meanings = meaningGroups
                        )

                        cacheMap[cleanWord] = result
                        DictionaryUiState.Success(result)
                    } else {
                        DictionaryUiState.NotFound
                    }
                }
                404 -> DictionaryUiState.NotFound
                429 -> DictionaryUiState.TooManyRequests
                500, 502, 503, 504 -> DictionaryUiState.Error("The server is experiencing issues (HTTP $responseCode). Please try again.")
                else -> DictionaryUiState.Error("HTTP Error: $responseCode")
            }
        } catch (e: java.net.UnknownHostException) {
            DictionaryUiState.NoInternet
        } catch (e: Exception) {
            DictionaryUiState.Error(e.localizedMessage ?: "Failed to retrieve data")
        }
    }
}