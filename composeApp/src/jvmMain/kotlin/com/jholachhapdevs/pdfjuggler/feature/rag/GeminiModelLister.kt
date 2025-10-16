package com.jholachhapdevs.pdfjuggler.feature.rag

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object GeminiModelLister {
    fun listModels(apiKey: String) {
        val url = "https://generativelanguage.googleapis.com/v1/models?key=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            println("[GeminiModelLister] HTTP status: ${response.code}")
            println("[GeminiModelLister] Response body: $body")
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val models = json.optJSONArray("models")
                if (models != null) {
                    for (i in 0 until models.length()) {
                        val model = models.getJSONObject(i)
                        println("Model: ${model.optString("name")}, Supported methods: ${model.optJSONArray("supportedGenerationMethods")}")
                    }
                } else {
                    println("No models found in response.")
                }
            } else {
                println("Failed to list models. Check your API key and network.")
            }
        }
    }
}

