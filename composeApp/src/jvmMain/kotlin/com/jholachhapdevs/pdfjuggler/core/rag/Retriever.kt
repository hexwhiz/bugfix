package com.jholachhapdevs.pdfjuggler.core.rag

/**
 * Interface for retrieving relevant text chunks
 */
interface Retriever {
    suspend fun retrieve(query: String, topK: Int = 3): List<String>
}
