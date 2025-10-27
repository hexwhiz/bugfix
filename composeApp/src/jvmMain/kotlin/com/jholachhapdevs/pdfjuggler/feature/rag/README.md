# RAG (Retrieval-Augmented Generation) Implementation

## Overview

This folder contains the RAG implementation for the PDF Juggler application, enabling intelligent question-answering capabilities over PDF documents. The system uses **LangChain4j** framework to provide a robust and production-ready RAG pipeline.

## Architecture

The RAG system uses the following components:

### 1. **LangChainRagEngine**
The main orchestrator that coordinates all RAG operations using LangChain4j's built-in capabilities.

### 2. **Embedding Model**
- **Model**: `text-embedding-004` (Google AI Gemini Embedding)
- **Purpose**: Converts text chunks into high-dimensional vector representations
- **Provider**: Google AI via LangChain4j integration

### 3. **Vector Store**
- **Implementation**: In-Memory Embedding Store (LangChain4j)
- **Purpose**: Stores and retrieves document embeddings efficiently
- **Features**: Fast similarity search using cosine distance

### 4. **Chat Model (LLM)**
- **Model**: `gemini-2.0-flash-exp` (Google AI Gemini)
- **Purpose**: Generates natural language answers based on retrieved context
- **Provider**: Google AI via LangChain4j integration

### 5. **Document Processing**
- **Parser**: Apache PDFBox Document Parser (LangChain4j)
- **Splitter**: Recursive text splitter (1000 tokens, 100 overlap)
- **Purpose**: Breaks PDF into manageable chunks for embedding

## How It Works

### Indexing Phase

1. **Document Loading**: PDF is loaded using LangChain4j's FileSystemDocumentLoader with PDFBox parser
2. **Text Splitting**: Document is split into chunks of ~1000 tokens with 100-token overlap for context preservation
3. **Embedding Generation**: Each chunk is converted to a vector embedding using Google's embedding model
4. **Storage**: Embeddings are stored in the in-memory vector store along with their text content

### Query Phase

1. **Query Embedding**: User's question is converted to a vector embedding
2. **Similarity Search**: Vector store finds the top K most relevant chunks (default K=3)
3. **Context Retrieval**: Retrieved chunks are assembled as context
4. **LLM Generation**: Context + question are sent to Gemini LLM
5. **Response**: LLM generates a natural language answer based on the context

## Usage

```kotlin
// Initialize RAG engine
val ragEngine = LangChainRagEngine(apiKey = "your-gemini-api-key")

// Index a PDF document
ragEngine.indexPdf("/path/to/document.pdf")

// Ask questions
val answer = ragEngine.answerQuery("What is the main topic of this document?")
```

## Key Features

- **Automatic PDF Processing**: Handles PDF parsing and chunking automatically
- **Smart Chunking**: Uses recursive splitting to maintain context boundaries
- **Efficient Retrieval**: Fast similarity search using cosine distance
- **Context-Aware Answers**: Provides answers grounded in document content
- **Production-Ready**: Built on LangChain4j's battle-tested framework

## Dependencies

The RAG system uses the following LangChain4j libraries:

- `dev.langchain4j:langchain4j:0.36.2` - Core framework
- `dev.langchain4j:langchain4j-google-ai-gemini:0.36.2` - Google AI Gemini integration
- `dev.langchain4j:langchain4j-embeddings:0.36.2` - Embedding model support
- `dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.36.2` - Additional embedding models

## API Configuration

The system requires a Google AI API key set in the environment variable `GEMINI_API_KEY`.

## Performance Considerations

- **Chunk Size**: 1000 tokens balances context preservation and retrieval precision
- **Chunk Overlap**: 100 tokens prevents information loss at chunk boundaries
- **Top-K Retrieval**: Default K=3 provides sufficient context without overwhelming the LLM
- **In-Memory Store**: Fast but limited to available RAM; suitable for typical PDF documents

## Future Enhancements

Potential improvements for the RAG system:

- Persistent vector storage (e.g., Chroma, Pinecone)
- Hybrid search (combining semantic and keyword search)
- Query reformulation for better retrieval
- Multi-document support
- Custom chunking strategies based on document structure

