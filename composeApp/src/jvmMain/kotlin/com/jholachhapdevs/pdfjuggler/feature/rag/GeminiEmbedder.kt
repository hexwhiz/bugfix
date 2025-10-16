package com.jholachhapdevs.pdfjuggler.feature.rag

import com.jholachhapdevs.pdfjuggler.core.rag.Embedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Gemini API Embedder implementation
 */
class GeminiEmbedder(private val apiKey: String) : Embedder {
    private val client = OkHttpClient()
    private val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=$apiKey"

    override suspend fun embed(text: String): List<Float>? = withContext(Dispatchers.IO) {
        println("[GeminiEmbedder] Embedding text of length ${text.length}...")
        val json = JSONObject()
            .put("model", "gemini-embedding-001")
            .put("content", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", text))))
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("[GeminiEmbedder] Embedding failed: HTTP ${response.code}")
                return@withContext null
            }
            val respJson = JSONObject(response.body?.string() ?: return@withContext null)
            val embeddings = respJson.optJSONObject("embedding")?.optJSONArray("values") ?: return@withContext null
            val result = List(embeddings.length()) { embeddings.getDouble(it).toFloat() }
            println("[GeminiEmbedder] Embedding success, vector size: ${result.size}")
            return@withContext result
        }
    }
}
