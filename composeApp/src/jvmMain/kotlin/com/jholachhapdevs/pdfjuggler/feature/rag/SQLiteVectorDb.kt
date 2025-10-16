package com.jholachhapdevs.pdfjuggler.feature.rag

import com.jholachhapdevs.pdfjuggler.core.rag.VectorDb
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.sqrt

/**
 * SQLite-based vector database for storing and querying embeddings
 */
class SQLiteVectorDb(dbPath: String = ":memory:") : VectorDb {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
    init {
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS embeddings (
                    id TEXT PRIMARY KEY,
                    embedding BLOB,
                    text TEXT
                )
            """)
        }
    }
    override fun addEmbedding(id: String, embedding: List<Float>, text: String) {
        val bytes = embedding.joinToString(",")
        val sql = "INSERT OR REPLACE INTO embeddings (id, embedding, text) VALUES (?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, id)
            ps.setString(2, bytes)
            ps.setString(3, text)
            ps.executeUpdate()
        }
        println("[SQLiteVectorDb] Added embedding for $id, text preview: ${text.take(100)}")
    }
    override fun query(queryEmbedding: List<Float>, topK: Int): List<Pair<String, String>> {
        val all = mutableListOf<Triple<String, List<Float>, String>>()
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT id, embedding, text FROM embeddings")
            while (rs.next()) {
                val id = rs.getString(1)
                val emb = rs.getString(2).split(",").mapNotNull { it.toFloatOrNull() }
                val text = rs.getString(3)
                all.add(Triple(id, emb, text))
            }
        }
        fun cosine(a: List<Float>, b: List<Float>): Float {
            val dot = a.zip(b).sumOf { (x, y) -> x.toDouble() * y.toDouble() }
            val normA = sqrt(a.sumOf { it.toDouble() * it.toDouble() })
            val normB = sqrt(b.sumOf { it.toDouble() * it.toDouble() })
            return if (normA == 0.0 || normB == 0.0) 0f else (dot / (normA * normB)).toFloat()
        }
        val results = all.map { Triple(it.first, cosine(queryEmbedding, it.second), it.third) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { Pair(it.first, it.third) }
        println("[SQLiteVectorDb] Query returned ${results.size} results for topK=$topK")
        results.forEach { (id, text) -> println("[SQLiteVectorDb] Result $id: ${text.take(100)}") }
        return results
    }
    override fun clear() {
        conn.createStatement().use { it.executeUpdate("DELETE FROM embeddings") }
    }
}
