package com.sillydevs.fetchapp

import retrofit2.http.GET

interface FetchApi {
    @GET("hiring.json") // Replace with your actual endpoint
    suspend fun getItems(): List<Item>
}