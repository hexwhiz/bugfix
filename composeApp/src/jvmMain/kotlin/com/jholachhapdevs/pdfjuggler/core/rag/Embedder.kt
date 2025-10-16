package com.jholachhapdevs.pdfjuggler.core.rag

/**
 * Interface for embedding text into vectors
 */
interface Embedder {
    suspend fun embed(text: String): List<Float>?
}
