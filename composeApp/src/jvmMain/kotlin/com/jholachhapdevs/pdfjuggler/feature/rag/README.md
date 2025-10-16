# Retrieval-Augmented Generation (RAG) in pdfjuggler

This document explains how RAG (Retrieval-Augmented Generation) is implemented in the `pdfjuggler` project, specifically in the `feature/rag` module.

## Overview
RAG combines information retrieval with generative AI. In this project, it allows the Gemini LLM to answer questions about the content of a PDF by first retrieving relevant text chunks from the PDF and then using those as context for generation.

## How it Works

1. **PDF Chunking**
   - When a PDF is loaded, it is split into text chunks (typically by page) using `PdfChunker`.

2. **Embedding**
   - Each chunk is converted into a vector (embedding) using the Gemini embedding API via `GeminiEmbedder`.

3. **Vector Database Storage**
   - Embeddings and their associated text are stored in a vector database (`SQLiteVectorDb`).

4. **Querying**
   - When a user asks a question, the question is embedded and the vector DB is queried for the most similar chunks (using cosine similarity).

5. **Context Construction**
   - The top relevant chunks are collected as context for the LLM.

6. **LLM Generation**
   - The context chunks and the user question are sent to the Gemini LLM (`GeminiLLM`). Each chunk is sent as a separate part, followed by the question.
   - The LLM generates an answer using both the context and its own knowledge.

## Key Classes
- `PdfChunker`: Splits PDF into text chunks.
- `GeminiEmbedder`: Converts text to embeddings using Gemini API.
- `SQLiteVectorDb`: Stores and retrieves embeddings and text.
- `RagRetriever`: Retrieves relevant chunks for a query.
- `GeminiLLM`: Calls Gemini LLM with context and question.
- `RagEngine`: Orchestrates the RAG pipeline.

## Typical Flow
1. User loads a PDF.
2. PDF is chunked and indexed (embedded and stored).
3. User asks a question.
4. System retrieves relevant chunks and sends them, along with the question, to the LLM.
5. LLM responds with an answer grounded in the PDF content.

## Troubleshooting
- If answers are generic or reference no PDF content, ensure the PDF is indexed and embeddings are being created and stored.
- If you see a warning like `[No context from PDF was found. Please ensure the PDF is indexed and try again.]`, check the embedding and vector DB steps.

---

For more details, see the code in this folder and the main project documentation.

