package com.jholachhapdevs.pdfjuggler.feature.rag

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * RAG Engine implementation using LangChain4j
 * Uses Gemini 2.5 Flash for LLM and Gemini Embedding 004 for embeddings
 */
class LangChainRagEngine(private val apiKey: String, private val modelName: String = "gemini-2.5-flash") {

    private val embeddingModel: EmbeddingModel = GoogleAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName("text-embedding-004")
        .build()

    private var chatModel: ChatLanguageModel = buildChatModel(modelName)

    private fun buildChatModel(model: String): ChatLanguageModel {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model)
            .build()
    }

    /**
     * Updates the chat model when the user changes model selection
     */
    fun updateModel(newModelName: String) {
        chatModel = buildChatModel(newModelName)
        println("[LangChainRagEngine] Updated chat model to: $newModelName")
    }

    private val embeddingStore: EmbeddingStore<TextSegment> = InMemoryEmbeddingStore()

    private val contentRetriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .maxResults(3)
        .build()

    // Chat memory to maintain conversation history (keeps last 10 messages)
    private val chatMemory: ChatMemory = MessageWindowChatMemory.withMaxMessages(10)

    /**
     * Indexes a PDF file by splitting it into chunks and storing embeddings
     */
    suspend fun indexPdf(pdfPath: String) = withContext(Dispatchers.IO) {
        try {
            val pdfFile = File(pdfPath)
            if (!pdfFile.exists()) {
                println("[LangChainRagEngine] PDF file not found: $pdfPath")
                return@withContext
            }

            // Extract text from PDF using PDFBox
            val pdfText = PDDocument.load(pdfFile).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }

            // Create a Document from the extracted text
            val document = Document.from(pdfText)

            // Split document into chunks (by page or by token count)
            val splitter = DocumentSplitters.recursive(1000, 100)
            val segments = splitter.split(document)

            println("[LangChainRagEngine] Loaded document with ${segments.size} segments")

            // Embed and store all segments
            segments.forEach { segment ->
                val embedding = embeddingModel.embed(segment).content()
                embeddingStore.add(embedding, segment)
            }

            println("[LangChainRagEngine] Successfully indexed PDF with ${segments.size} chunks")
        } catch (e: Exception) {
            val cleanErrorMessage = extractErrorMessage(e)
            println("[LangChainRagEngine] Error indexing PDF: $cleanErrorMessage")
            e.printStackTrace()
        }
    }

    /**
     * Answers a query using RAG (retrieves context, then calls LLM)
     */
    suspend fun answerQuery(query: String, topK: Int = 3): String = withContext(Dispatchers.IO) {
        try {
            // Retrieve relevant content from the PDF
            val relevantContents = contentRetriever.retrieve(dev.langchain4j.rag.query.Query.from(query))

            if (relevantContents.isEmpty()) {
                return@withContext "[No context from PDF was found. Please ensure the PDF is indexed and try again.]"
            }

            println("[LangChainRagEngine] Retrieved ${relevantContents.size} context chunks for query: '$query'")

            // Build context from retrieved documents
            val contextText = relevantContents.joinToString("\n\n") { content ->
                "Context from PDF: ${content.textSegment().text()}"
            }

            // Create the user message with both context and query
            val userMessageText = """
                Based on the following context from the PDF:
                
                $contextText
                
                Question: $query
                
                Please answer the question based on the context provided and our conversation history.
            """.trimIndent()

            // Add user message to chat memory
            chatMemory.add(UserMessage.from(userMessageText))

            // Generate response using LLM with full conversation history
            val messages = chatMemory.messages()
            val response = chatModel.generate(messages)

            // Extract AI message and text from response
            val aiMessage = response.content()
            val responseText = aiMessage.text()

            // Add AI response to chat memory
            chatMemory.add(aiMessage)

            println("[LangChainRagEngine] Generated response with conversation history (${messages.size} messages in context)")

            return@withContext responseText
        } catch (e: Exception) {
            println("[LangChainRagEngine] Error answering query: ${e.message}")
            e.printStackTrace()

            // Extract a clean error message from the exception
            val cleanErrorMessage = extractErrorMessage(e)
            return@withContext "[Error: $cleanErrorMessage]"
        }
    }

    /**
     * Extracts a clean error message from an exception that may contain JSON error responses
     */
    private fun extractErrorMessage(exception: Exception): String {
        val message = exception.message ?: return "Unknown error occurred"

        // First, try to extract the actual message from nested JSON
        // Pattern 1: "message": "actual error message with details..."
        val detailedMessagePattern = """"message":\s*"([^"]+(?:\\.[^"]+)*)"""".toRegex()
        val detailedMatch = detailedMessagePattern.find(message)

        if (detailedMatch != null) {
            val extractedMessage = detailedMatch.groupValues[1]
            // Clean up escaped characters and split on newlines
            val cleanedMessage = extractedMessage
                .replace("""\\n""", " ")
                .replace("""\\"""", "\"")
                .replace("""\n""", " ")
                .trim()

            // Extract just the first sentence (before the first period followed by space or end)
            val firstSentence = cleanedMessage.split(Regex("""\.\s+""")).firstOrNull() ?: cleanedMessage
            return if (firstSentence.length > 150) {
                firstSentence.take(150) + "..."
            } else {
                firstSentence
            }
        }

        // Pattern 2: If message contains JSON with error code
        if (message.contains(""""error"""") && message.contains("{")) {
            // Try to find HTTP error codes and provide user-friendly messages
            if (message.contains("429")) {
                return "Rate limit exceeded. Please try again later or check your API quota."
            }
            if (message.contains("401") || message.contains("403")) {
                return "Authentication error. Please check your API key."
            }
            if (message.contains("400")) {
                return "Bad request. Please try rephrasing your query."
            }
            if (message.contains("500") || message.contains("503")) {
                return "Service temporarily unavailable. Please try again later."
            }
        }

        // If the message contains "HTTP error" extract just that part
        if (message.contains("HTTP error")) {
            val httpErrorPattern = """HTTP error \((\d+)\)""".toRegex()
            val httpMatch = httpErrorPattern.find(message)
            if (httpMatch != null) {
                val statusCode = httpMatch.groupValues[1]
                return when (statusCode) {
                    "429" -> "Rate limit exceeded. Please try again later."
                    "401", "403" -> "Authentication error. Please check your API key."
                    "400" -> "Bad request. Please try rephrasing your query."
                    "500", "503" -> "Service temporarily unavailable. Please try again later."
                    else -> "HTTP error ($statusCode). Please try again."
                }
            }
        }

        // If it looks like JSON or RuntimeException wrapper, return a generic message
        if (message.trim().startsWith("{") || message.contains("RuntimeException") || message.contains("Exception")) {
            return "An error occurred while processing your request. Please try again."
        }

        // Return the original message if it's already clean (limit length)
        return if (message.length > 150) {
            message.take(150) + "..."
        } else {
            message
        }
    }

    /**
     * Clears the embedding store and conversation memory
     */
    fun clear() {
        embeddingStore.removeAll()
        chatMemory.clear()
        println("[LangChainRagEngine] Cleared embedding store and conversation memory")
    }

    /**
     * Clears only the conversation memory (keeps the indexed PDF)
     */
    fun clearConversation() {
        chatMemory.clear()
        println("[LangChainRagEngine] Cleared conversation memory")
    }
}

