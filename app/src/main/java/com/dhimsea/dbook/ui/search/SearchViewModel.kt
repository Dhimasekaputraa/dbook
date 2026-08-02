package com.dhimsea.dbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhimsea.dbook.domain.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class SearchViewModel : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun parseSearchResults(jsonResults: String) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonResults)
                val list = mutableListOf<SearchResult>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SearchResult(
                            cfi = obj.getString("cfi"),
                            excerpt = obj.getString("excerpt"),
                            chapter = obj.getString("chapter"),
                            page = obj.getInt("page")
                        )
                    )
                }
                _searchResults.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}