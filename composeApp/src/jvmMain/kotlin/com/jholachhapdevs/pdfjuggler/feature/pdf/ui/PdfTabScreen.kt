// Kotlin
package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.core.util.LocalComposeWindow
import com.jholachhapdevs.pdfjuggler.feature.pdf.data.repository.PdfFileRepository
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.usecase.GetPdfUseCase
import com.jholachhapdevs.pdfjuggler.core.util.Resources

data class PdfTabScreen(
    val pdfFile: PdfFile
) : Screen {

    @Composable
    override fun Content() {
        val window = LocalComposeWindow.current

        // Load persisted AI model
        var initialAiModel by remember { mutableStateOf(Resources.DEFAULT_AI_MODEL) }
        LaunchedEffect(Unit) {
            initialAiModel = com.jholachhapdevs.pdfjuggler.core.datastore.AiModelStore.getSelectedModel()
        }

        val model = rememberScreenModel {
            PdfTabScreenModel(
                getPdfUseCase = GetPdfUseCase(PdfFileRepository(window)),
                window = window,
                initial = pdfFile,
                currentAiModel = initialAiModel
            )
        }

        PdfTabComponent(model)
    }
}