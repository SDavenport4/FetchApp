package com.sillydevs.fetchapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoadingState {
    Idle,
    Loading,
    Success,
    Error
}

data class UiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val items: Map<Int, List<Item>> = emptyMap(),
    val expandedStates: Map<Int, Boolean> = emptyMap()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchApi: FetchApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.onStart {
        fetchItems()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UiState()
    )

    fun toggleGroupExpansion(listId: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                expandedStates = currentState.expandedStates + (listId to !(currentState.expandedStates[listId] ?: true))
            )
        }
    }


    fun fetchItems() {
        viewModelScope.launch {
            try {
                updateLoadingState(LoadingState.Loading)
                val response = fetchApi.getItems()

                val filteredItems = response.filter { !it.name.isNullOrBlank() }

                /** This could be used for TRUE sorting by name **/
                //val sortedItems = filteredItems.sortedWith(
                //    compareBy<Item> { it.listId }.thenBy { it.name }
                //)

                /** This will sort items by name, respecting the number in their name, and placing at the end if no number provided.**/
                val sortedItems = filteredItems.sortedWith(
                    compareBy<Item> { it.listId }.thenBy {
                        val numberPart = it.name?.substringAfter("Item ")?.toIntOrNull() ?: Int.MAX_VALUE
                        numberPart
                    }
                )

                val groupedItems = sortedItems.groupBy { it.listId }

                _uiState.update { currentState ->
                    currentState.copy(
                        items = groupedItems,
                        loadingState = LoadingState.Success
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching items", e)
                updateLoadingState(LoadingState.Error)
            }
        }
    }


    private fun updateLoadingState(loadingState: LoadingState) {
        _uiState.update { currentState ->
            currentState.copy(
                loadingState = loadingState
            )
        }
    }
}