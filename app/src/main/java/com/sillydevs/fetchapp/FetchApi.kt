package com.sillydevs.fetchapp

import retrofit2.http.GET

interface FetchApi {
    @GET("hiring.json")
    suspend fun getItems(): List<Item>
}