package com.jholachhapdevs.pdfjuggler.feature.rag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini LLM API caller for text generation
 */
class GeminiLLM(private val apiKey: String) {
    private val client = OkHttpClient()
    // Updated to v1 and correct model name for Gemini 2.5 Flash-Lite
    private val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash-lite:generateContent?key=$apiKey"

    /**
     * Sends a prompt (query + context) to Gemini LLM and returns the response
     * Each context chunk is sent as a separate part, followed by the user question.
     */
    suspend fun generate(query: String, context: List<String>): String? = withContext(Dispatchers.IO) {
        // If no context, warn and return a helpful message
        if (context.isEmpty()) {
            println("[GeminiLLM] No context chunks provided to LLM. Returning warning.")
            return@withContext "[No context from PDF was found. Please ensure the PDF is indexed and try again.]"
        }
        // Build the contents array: each context chunk as a part, then the question as a part
        val contentsArray = JSONArray()
        context.forEachIndexed { i, chunk ->
            contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", "Context chunk ${i + 1}:\n$chunk"))))
        }
        // Add the user question as the final part
        contentsArray.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", "Question: $query"))))

        val json = JSONObject().put("contents", contentsArray)
        println("[GeminiLLM] Sending structured context to Gemini:\n$json")
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            println("[GeminiLLM] HTTP status: ${response.code}")
            println("[GeminiLLM] Response body: $responseBody")
            if (!response.isSuccessful) return@withContext null
            val respJson = JSONObject(responseBody ?: return@withContext null)
            val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null
            return@withContext parts.getJSONObject(0).optString("text")
        }
    }
}
