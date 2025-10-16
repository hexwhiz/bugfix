package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.AttachedFile
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.UploadFileUseCase
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.TabScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AiScreenModel(
    val pdfFile: PdfFile,
    private val tabScreenModel: TabScreenModel, // Add reference to TabScreenModel
    private val uploadFileUseCase: UploadFileUseCase,
    initialSelectedPageIndex: Int,
    val assistantName: String = "Ringmaster"
) : ScreenModel {

    companion object {
        private const val MIME_PDF = "application/pdf"
    }

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var currentJob: Job? = null

    // Cache: uploaded PDF fileUri
    private var pdfFileUri: String? = null

    // Current page index (for UI / optional prompt context)
    private var selectedPageIndex: Int = initialSelectedPageIndex

    init {
        // Upload the PDF once in the background
        screenModelScope.launch {
            try {
                ensurePdfFileAttachment()
            } catch (t: Throwable) {
                uiState = uiState.copy(error = "PDF upload failed: ${t.message}")
            }
        }
    }

    fun setSelectedPage(index: Int) {
        selectedPageIndex = index
    }

    fun updateInput(text: String) {
        uiState = uiState.copy(input = text)
    }

    fun send() {
        val prompt = uiState.input.trim()
        if (prompt.isBlank() || uiState.isSending) return

        val userMessage = ChatMessage(role = "user", text = prompt)
        uiState = uiState.copy(
            messages = uiState.messages + userMessage,
            input = "",
            isSending = true,
            error = null
        )

        currentJob = screenModelScope.launch {
            try {
                // Use RAG pipeline for PDF Q&A
                val answer = tabScreenModel.answerUserQuery(prompt)
                val reply = ChatMessage(role = "assistant", text = answer)
                uiState = uiState.copy(messages = uiState.messages + reply, isSending = false)
            } catch (t: Throwable) {
                uiState = uiState.copy(isSending = false, error = t.message ?: "Failed to send")
            }
        }
    }

    fun cancelSending() {
        val job = currentJob ?: return
        screenModelScope.launch {
            try {
                job.cancelAndJoin()
            } catch (_: Throwable) {
            }
            uiState = uiState.copy(isSending = false)
        }
    }

    fun clearMessages() {
        if (uiState.isSending) return
        uiState = uiState.copy(messages = emptyList())
    }

    fun newChat() {
        if (uiState.isSending) return
        uiState = ChatUiState()
    }

    fun dismissError() {
        uiState = uiState.copy(error = null)
    }

    /**
     * Process a pending AI request from TabScreenModel
     * This sends a constrained prompt based on the mode without showing the prompt in chat
     */
    fun processPendingRequest(
        text: String,
        mode: com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.AiRequestMode
    ) {
        if (uiState.isSending) return

        val (prompt, _) = when (mode) {
            com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.AiRequestMode.Dictionary -> {
                Pair("What does '$text' mean?", false)
            }
            com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.AiRequestMode.Translate -> {
                Pair(
                    "Please translate ONLY this specific text: '$text'\n\nWhat language would you like me to translate it to? Please specify the target language, and I'll provide the translation for just this selected text.",
                    false
                )
            }
        }

        uiState = uiState.copy(
            isSending = true,
            error = null
        )

        currentJob = screenModelScope.launch {
            try {
                // Use RAG pipeline for dictionary/translate
                val answer = tabScreenModel.answerUserQuery(prompt)
                val reply = ChatMessage(role = "assistant", text = answer)
                uiState = uiState.copy(
                    messages = uiState.messages + reply,
                    isSending = false
                )
            } catch (t: Throwable) {
                uiState = uiState.copy(isSending = false, error = t.message ?: "Failed to send")
            }
        }
    }

    /**
     * Generate a comprehensive cheat sheet with key terms, definitions, and important concepts
     */
    fun generateCheatSheet() {
        if (uiState.isSending) return

        val prompt = """
            Create a comprehensive cheat sheet for this PDF document that includes:
            
            ## 📚 KEY TERMS
            - List the most important terms and vocabulary
            
            ## 📖 DEFINITIONS
            - Provide clear, concise definitions for key concepts
            
            ## 💡 IMPORTANT CONCEPTS
            - Highlight the main ideas and principles
            - Include any formulas, theories, or frameworks
            
            Please format this as a well-structured study guide that would be helpful for review and reference.
        """.trimIndent()

        // Start generating cheat sheet
        uiState = uiState.copy(
            isSending = true,
            error = null
        )

        currentJob = screenModelScope.launch {
            try {
                // Use RAG pipeline for cheat sheet generation
                val answer = tabScreenModel.answerUserQuery(prompt)
                val reply = ChatMessage(role = "assistant", text = answer)
                uiState = uiState.copy(
                    messages = uiState.messages + reply,
                    isSending = false
                )
            } catch (t: Throwable) {
                uiState = uiState.copy(isSending = false, error = t.message ?: "Failed to generate cheat sheet")
            }
        }
    }

    private suspend fun ensurePdfFileAttachment(): AttachedFile? {
        val cached = pdfFileUri
        if (cached != null) {
            return AttachedFile(mimeType = MIME_PDF, fileUri = cached)
        }
        return try {
            val bytes = withContext(Dispatchers.IO) { File(pdfFile.path).readBytes() }
            val uri = uploadFileUseCase(pdfFile.name, MIME_PDF, bytes)
            pdfFileUri = uri
            AttachedFile(mimeType = MIME_PDF, fileUri = uri)
        } catch (_: Throwable) {
            null
        }
    }
}