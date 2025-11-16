package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.datastore.PrefsManager
import kotlinx.coroutines.launch
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import java.io.File

@Composable
fun PdfComponent(
    screenModel: PdfScreenModel
) {
    val cs = MaterialTheme.colorScheme
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    var recentPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        recentPaths = com.jholachhapdevs.pdfjuggler.core.datastore.RecentPdfsStore.getRecentPaths()
    }

    LaunchedEffect(screenModel.pickedFile) {
        val file = screenModel.consumePickedFile() ?: return@LaunchedEffect
        // Navigate immediately; repository handles MRU saving in background
        navigator.push(PdfTabScreen(file))
        // Optionally refresh recent list without blocking UI
        scope.launch {
            try {
                recentPaths = com.jholachhapdevs.pdfjuggler.core.datastore.RecentPdfsStore.getRecentPaths()
            } catch (_: Throwable) {}
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(24.dp)
            .padding(top = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        // Settings button (top-right) outside the main card
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val hasKey = screenModel.apiKey.isNotBlank()
                JButton(
                    onClick = { screenModel.openApiKeyDialog() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(if (hasKey) "Change API Key" else "Add API Key")
                }
                if (!hasKey) {
                    JText(
                        text = "AI features are disabled — no API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Surface(
            color = cs.background,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.40f)),
            modifier = Modifier
                .widthIn(min = 460.dp)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // HEADER
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    JText(
                        text = "Load PDF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = cs.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(56.dp)
                            .background(cs.primary.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.height(10.dp))
                    JText(
                        text = "Pick a PDF to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onBackground.copy(alpha = 0.85f)
                    )
                }

                // MAIN BUTTON
                JButton(onClick = { screenModel.pickPdfFile() }) {
                    Text(
                        text = "Pick from This PC",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Recent files
                if (recentPaths.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        JText(
                            text = "Recent files",
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onBackground.copy(alpha = 0.9f)
                        )
                        recentPaths.forEach { path ->
                            val name = remember(path) { File(path).name }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                JButton(onClick = {
                                    // Open recent file (no picker, navigate directly)
                                    val file = com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile(
                                        path = path,
                                        name = name
                                    )
                                    navigator.push(PdfTabScreen(file))
                                }) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }

            }
        }
        // API Key Dialog
        if (screenModel.showApiKeyDialog) {
            ApiKeyDialog(
                apiKey = screenModel.apiKey,
                onSave = { key ->
                    screenModel.saveApiKey(key)
                    screenModel.closeApiKeyDialog()
                },
                onClear = {
                    screenModel.clearApiKey()
                    screenModel.closeApiKeyDialog()
                },
                onDismiss = { screenModel.closeApiKeyDialog() }
            )
        }
    }
}