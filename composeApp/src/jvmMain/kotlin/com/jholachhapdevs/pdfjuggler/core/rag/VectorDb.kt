package com.jholachhapdevs.pdfjuggler.core.rag

/**
 * Interface for a vector database to store and retrieve embeddings
 */
interface VectorDb {
    fun addEmbedding(id: String, embedding: List<Float>, text: String)
    fun query(queryEmbedding: List<Float>, topK: Int = 3): List<Pair<String, String>> // Returns (id, text)
    fun clear()
}
