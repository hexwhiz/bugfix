package com.jholachhapdevs.pdfjuggler.feature.rag

import com.jholachhapdevs.pdfjuggler.core.rag.Embedder
import com.jholachhapdevs.pdfjuggler.core.rag.Retriever
import com.jholachhapdevs.pdfjuggler.core.rag.VectorDb

/**
 * RAG Engine: builds context and sends to LLM
 */
class RagEngine(
    private val retriever: Retriever,
    private val embedder: Embedder,
    private val vectorDb: VectorDb,
    private val llm: GeminiLLM // Add GeminiLLM as a dependency
) {
    /**
     * Indexes a PDF file: splits, embeds, and stores in vector DB
     */
    suspend fun indexPdf(pdfPath: String) {
        val chunks = PdfChunker.chunkByPage(pdfPath)
        for ((page, text) in chunks) {
            val embedding = embedder.embed(text) ?: continue
            vectorDb.addEmbedding("page_$page", embedding, text)
        }
    }

    /**
     * Answers a query using RAG (retrieves context, then calls LLM)
     */
    suspend fun answerQuery(query: String, topK: Int = 3): String {
        val contextChunks = retriever.retrieve(query, topK)
        println("[RagEngine] Retrieved ${contextChunks.size} context chunks for query: '$query'")
        contextChunks.forEachIndexed { i, chunk ->
            println("[RagEngine] Context chunk $i: ${chunk.take(200)}...")
        }
        val llmAnswer = llm.generate(query, contextChunks) ?: "[No answer from LLM]"
        return llmAnswer
    }
}
