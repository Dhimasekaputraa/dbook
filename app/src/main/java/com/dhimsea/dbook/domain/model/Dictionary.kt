package com.dhimsea.dbook.domain.model

import com.google.gson.annotations.SerializedName

data class DictionaryResponse(
    val word: String,
    val phonetic: String?,
    val phonetics: List<Phonetic>?,
    val meanings: List<Meaning>
)

data class Phonetic(
    val text: String?,
    val audio: String?
)

data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>,
    val synonyms: List<String>?,
    val antonyms: List<String>?
)

data class Definition(
    val definition: String,
    val example: String?,
    val synonyms: List<String>?,
    val antonyms: List<String>?
)

data class DefinitionItem(
    val definition: String,
    val example: String? = null
)

data class MeaningGroup(
    val partOfSpeech: String,
    val definitions: List<DefinitionItem>,
    val synonyms: List<String> = emptyList()
)

data class DefinitionResult(
    val word: String,
    val phonetic: String?,
    val meanings: List<MeaningGroup>
)

sealed interface DictionaryUiState {
    object Loading : DictionaryUiState
    data class Success(val data: DefinitionResult) : DictionaryUiState
    object NotFound : DictionaryUiState
    object NoInternet : DictionaryUiState
    object TooManyRequests : DictionaryUiState
    data class Error(val message: String) : DictionaryUiState
}