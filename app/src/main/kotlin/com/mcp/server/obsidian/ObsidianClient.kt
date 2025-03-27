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
import java.time.Instant

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
        val endpoint = if (path == null) "/vault" else "/vault/$path"
        return try {
            val response: VaultListResponse = client.get("$baseUrl$endpoint/") {
                header("Authorization", "Bearer $apiKey")
            }.body()
            response.files
        } catch (e: Exception) {
            logger.error(e) { "Failed to list files at path: $path" }
            emptyList()
        }
    }

    /**
     * Gets the content of a file in the vault.
     *
     * @param path The path to the file within the vault.
     * @return The content of the file as a string, or null if the file does not exist.
     */
    suspend fun getFileContent(path: String): String? {
        return try {
            val response: FileContentResponse = client.get("$baseUrl/vault/$path/") {
                header("Authorization", "Bearer $apiKey")
            }.body()
            response.content
        } catch (e: Exception) {
            logger.error(e) { "Failed to get content of file: $path" }
            null
        }
    }

    /**
     * Creates a new file in the vault.
     *
     * @param path The path where the file should be created.
     * @param content The content of the file.
     * @return True if the file was created successfully, false otherwise.
     */
    suspend fun createFile(path: String, content: String): Boolean {
        return try {
            val response = client.put("$baseUrl/vault/$path/") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Text.Plain)
                setBody(content)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error(e) { "Failed to create file: $path" }
            false
        }
    }

    /**
     * Updates an existing file in the vault.
     *
     * @param path The path to the file within the vault.
     * @param content The new content of the file.
     * @return True if the file was updated successfully, false otherwise.
     */
    suspend fun updateFile(path: String, content: String): Boolean {
        return try {
            val response = client.post("$baseUrl/vault/$path/") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Text.Plain)
                setBody(content)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error(e) { "Failed to update file: $path" }
            false
        }
    }

    /**
     * Gets the contents of multiple files in the vault.
     *
     * @param paths List of file paths to retrieve.
     * @return A map of file paths to their contents, excluding any files that could not be retrieved.
     */
    suspend fun getBatchFileContents(paths: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (path in paths) {
            getFileContent(path)?.let { content ->
                result[path] = content
            }
        }
        return result
    }

    /**
     * Gets recent changes in the vault.
     *
     * @param limit Maximum number of files to return (default is 10).
     * @param days Only include files modified within this many days (default is 30).
     * @return A list of recently modified files with their metadata.
     */
    suspend fun getRecentChanges(limit: Int = 10, days: Int = 30): List<FileMetadata> {
        try {
            // Get all files in the vault
            val allFiles = listFiles()
            // For now, this is a mock implementation since the Local REST API doesn't provide modification times
            // In a real implementation, you would use the API to get file metadata

            // Mock data for demonstration
            val now = Instant.now()
            return allFiles.take(limit).mapIndexed { index, path ->
                FileMetadata(
                    path = path,
                    modified = now.minusSeconds((index * 86400).toLong()).toString(),
                    created = now.minusSeconds((index * 86400 * 2).toLong()).toString()
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get recent changes" }
            return emptyList()
        }
    }
}

@Serializable
data class VaultListResponse(
    val files: List<String>
)

@Serializable
data class FileContentResponse(
    val content: String
)

@Serializable
data class FileMetadata(
    val path: String,
    val modified: String,
    val created: String
)
