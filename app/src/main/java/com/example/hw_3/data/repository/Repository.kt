package com.example.hw_3.data.repository

import com.example.hw_3.data.api.RetrofitClient
import com.example.hw_3.data.models.Character
import com.example.hw_3.data.models.CharacterResponse

class Repository {
    private val apiService = RetrofitClient.apiService

    suspend fun getCharacters(page: Int = 1): CharacterResponse {
        return apiService.getCharacters(page)
    }

    suspend fun getCharacterById(id: Int): Character {
        return apiService.getCharacterById(id)
    }
}