package com.example.hw_3.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import com.example.hw_3.data.models.Character
import com.example.hw_3.data.repository.Repository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.async

sealed class ListUiState {
    object Loading : ListUiState()
    data class Success(val characters: List<Character>) : ListUiState()
    data class Error(val message: String) : ListUiState()
    object Empty : ListUiState()
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val character: Character) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class CharactersViewModel : ViewModel() {
    private val repository = Repository()

    private val scope = CoroutineScope(
        Job() +
                Dispatchers.IO +
                CoroutineName("characters-scope")
    )

    var listState by mutableStateOf<ListUiState>(ListUiState.Loading)
        private set

    var detailState by mutableStateOf<DetailUiState>(DetailUiState.Loading)
        private set

    var searchQuery by mutableStateOf("")
        private set


    fun loadCharacters() {
        scope.launch(CoroutineName("load-characters")) {
            try {
                listState = ListUiState.Loading


                val deferred1 = async(CoroutineName("load-page-1")) {
                    println("load-page-1: thread=${Thread.currentThread().name}")
                    repository.getCharacters(page = 1)
                }

                val deferred2 = async(CoroutineName("load-page-2")) {
                    println("load-page-2: thread=${Thread.currentThread().name}")
                    repository.getCharacters(page = 2)
                }


                val result1 = deferred1.await()
                val result2 = deferred2.await()

                val allCharacters = result1.results + result2.results

                listState = if (allCharacters.isEmpty()) {
                    ListUiState.Empty
                } else {
                    ListUiState.Success(allCharacters)
                }
            } catch (e: Exception) {
                listState = ListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }


    fun searchCharacters(query: String) {
        searchQuery = query
        scope.launch(CoroutineName("search-characters")) {
            try {
                println("search-characters started on thread=${Thread.currentThread().name}")

                delay(300)

                listState = ListUiState.Loading
                val response = repository.getCharacters()
                val filtered = response.results.filter {
                    it.name.contains(query, ignoreCase = true)
                }

                listState = if (filtered.isEmpty()) {
                    ListUiState.Empty
                } else {
                    ListUiState.Success(filtered)
                }

                println("search-characters finished")
            } catch (e: Exception) {
                listState = ListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCharacterDetail(characterId: Int) {
        scope.launch(CoroutineName("load-detail")) {
            try {
                println("load-detail: characterId=$characterId, thread=${Thread.currentThread().name}")

                delay(300)

                detailState = DetailUiState.Loading
                val character = repository.getCharacterById(characterId)
                detailState = DetailUiState.Success(character)

                println("load-detail finished")
            } catch (e: Exception) {
                detailState = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun retry() {
        if (searchQuery.isNotEmpty()) {
            searchCharacters(searchQuery)
        } else {
            loadCharacters()
        }
    }

    fun retryDetail(characterId: Int) {
        loadCharacterDetail(characterId)
    }


    override fun onCleared() {
        super.onCleared()
        println("cancel whole scope")
        scope.cancel()
    }
}