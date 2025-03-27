package com.mcp.server.obsidian

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Client for communicating with the Obsidian Local REST API.
 *
 * @param baseUrl The base URL of the Obsidian REST API.
 * @param apiKey The API key for authenticating with the Obsidian REST API.
 */
class ObsidianClient(
    private val baseUrl: String,
    private val apiKey: String
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Lists all files and directories in the vault or in a specific directory.
     *
     * @param path Optional path within the vault. If null, lists files in the vault root.
     * @return A list of vault items (files and directories).
     */
    suspend fun listFiles(path: String? = null): List<String> {
        val endpoint = if (path == null) "/vault/" else "/vault/$path/"
        return try {
            val response: VaultListResponse = client.get("$baseUrl$endpoint") {
                header("Authorization", "Bearer $apiKey")
            }.body()
            response.files
        } catch (e: Exception) {
            logger.error(e) { "Failed to list files at path: $path" }
            emptyList()
        }
    }
}

@Serializable
data class VaultListResponse(
    val files: List<String>
)
