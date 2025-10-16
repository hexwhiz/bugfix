package com.jholachhapdevs.pdfjuggler.feature.rag

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

/**
 * Splits PDF into text chunks (by page)
 */
object PdfChunker {
    fun chunkByPage(pdfPath: String): List<Pair<Int, String>> {
        val doc = PDDocument.load(java.io.File(pdfPath))
        val stripper = PDFTextStripper()
        val chunks = mutableListOf<Pair<Int, String>>()
        for (i in 0 until doc.numberOfPages) {
            stripper.startPage = i + 1
            stripper.endPage = i + 1
            val text = stripper.getText(doc).trim()
            println("[PdfChunker] Page ${i + 1}: ${text.take(200)}...")
            if (text.isNotEmpty()) chunks.add(i to text)
        }
        doc.close()
        println("[PdfChunker] Extracted " + chunks.size + " non-empty pages from $pdfPath")
        return chunks
    }
}
