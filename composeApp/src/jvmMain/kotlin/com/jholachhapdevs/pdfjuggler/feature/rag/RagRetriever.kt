package com.jholachhapdevs.pdfjuggler.feature.rag

import com.jholachhapdevs.pdfjuggler.core.rag.Embedder
import com.jholachhapdevs.pdfjuggler.core.rag.Retriever
import com.jholachhapdevs.pdfjuggler.core.rag.VectorDb

/**
 * Retriever implementation using an embedder and a vector DB
 */
class RagRetriever(
    private val embedder: Embedder,
    private val vectorDb: VectorDb
) : Retriever {
    override suspend fun retrieve(query: String, topK: Int): List<String> {
        val queryEmbedding = embedder.embed(query) ?: return emptyList()
        return vectorDb.query(queryEmbedding, topK).map { it.second }
    }
}
