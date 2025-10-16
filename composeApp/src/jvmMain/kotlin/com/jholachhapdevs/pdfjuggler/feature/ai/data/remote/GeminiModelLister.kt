package com.jholachhapdevs.pdfjuggler.feature.ai.data.remote

import com.jholachhapdevs.pdfjuggler.core.networking.httpClient
import com.jholachhapdevs.pdfjuggler.core.util.Env
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object GeminiModelLister {
    private const val listModelsUrl = "https://generativelanguage.googleapis.com/v1/models"

    @Serializable
    data class Model(
        val name: String,
        val displayName: String? = null,
        val description: String? = null,
        val supportedGenerationMethods: List<String>? = null
    )

    @Serializable
    data class ListModelsResponse(
        val models: List<Model> = emptyList()
    )

    suspend fun listModels(apiKey: String = Env.GEMINI_API_KEY): List<Model> {
        val response: ListModelsResponse = httpClient.get(listModelsUrl) {
            parameter("key", apiKey)
        }.body()
        return response.models
    }
}

