package com.sillydevs.fetchapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchApi: FetchApi
) : ViewModel() {

    // Holds all items
    private val _items = MutableStateFlow<Map<Int, List<Item>>>(emptyMap())
    val items = _items.onStart {
        fetchItems()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyMap()
    )

    // Holds expanded state for each listId
    private val _expandedStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val expandedStates = _expandedStates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyMap()
    )

    fun toggleGroupExpansion(listId: Int) {
        val currentStates = _expandedStates.value.toMutableMap()
        val currentState = currentStates[listId] ?: true
        currentStates[listId] = !currentState
        _expandedStates.value = currentStates
    }

    // Only called when the #collectAsState is called for the items
    private fun fetchItems() {
        viewModelScope.launch {
            try {
                val response = fetchApi.getItems()

                val filteredItems = response.filter { !it.name.isNullOrBlank() }

                val sortedItems = filteredItems.sortedWith(
                    compareBy<Item> { it.listId }.thenBy { it.name }
                )

                val groupedItems = sortedItems.groupBy { it.listId }

                _items.value = groupedItems
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching items", e)
            }
        }
    }
}