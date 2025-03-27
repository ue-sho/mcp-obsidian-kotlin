package com.mcp.server.mcp

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import com.mcp.server.obsidian.ObsidianClient
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered

private val logger = KotlinLogging.logger {}

/**
 * MCP Server for Obsidian integration.
 *
 * @param apiKey The API key for the Obsidian REST API.
 * @param obsidianUrl The URL of the Obsidian REST API. Default is http://localhost:27123.
 */
class ObsidianMcpServer(
    private val apiKey: String,
    private val obsidianUrl: String
) {
    private val obsidianClient = ObsidianClient(obsidianUrl, apiKey)
    private val server = Server(
        serverInfo = Implementation(
            name = "mcp-obsidian-kotlin",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(
                    subscribe = true,
                    listChanged = true
                ),
                tools = ServerCapabilities.Tools(
                    listChanged = true
                )
            )
        )
    )

    init {
        // Register tools
        registerTools()
    }

    /**
     * Starts the MCP server using the stdio transport.
     */
    fun start() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val transport = StdioServerTransport(
                    inputStream = System.`in`.asSource().buffered(),
                    outputStream = System.out.asSink().buffered()
                )
                server.connect(transport)
            } catch (e: Exception) {
                logger.error(e) { "Error in MCP server" }
            }
        }
    }

    /**
     * Registers all tools with the MCP server.
     */
    private fun registerTools() {
        // List files in vault
        server.addTool(
            name = "mcp_obsidian_list_files_in_vault",
            description = "Lists all files and directories in the root directory of your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                val files = runBlocking { obsidianClient.listFiles() }
                val result = JsonArray(files.map {
                    JsonObject(
                        mapOf(
                            "file" to JsonPrimitive(it),
                        )
                    )
                })
                CallToolResult(
                    content = listOf(TextContent(result.toString()))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error listing files in vault" }
                throw e
            }
        }

        // List files in directory
        server.addTool(
            name = "mcp_obsidian_list_files_in_dir",
            description = "Lists all files and directories in a specific directory of your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                val path = jsonElement?.get("path")?.jsonPrimitive?.content ?: ""

                val files = runBlocking { obsidianClient.listFiles(path) }
                val result = JsonArray(files.map {
                    JsonObject(
                        mapOf(
                            "file" to JsonPrimitive(it),
                        )
                    )
                })
                CallToolResult(
                    content = listOf(TextContent(result.toString()))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error listing files in directory: ${e.message}" }
                throw e
            }
        }

        // Get file content
        server.addTool(
            name = "mcp_obsidian_get_file_content",
            description = "Gets the content of a file in your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                val path = jsonElement?.get("path")?.jsonPrimitive?.content ?: ""

                val content = runBlocking { obsidianClient.getFileContent(path) }
                    ?: return@addTool CallToolResult(
                        content = listOf(TextContent("File not found: $path"))
                    )

                CallToolResult(
                    content = listOf(TextContent(content))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting file content: ${e.message}" }
                throw e
            }
        }

        // Create file
        server.addTool(
            name = "mcp_obsidian_create_file",
            description = "Creates a new file in your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                    ?: throw IllegalArgumentException("Invalid request format")
                val path = jsonElement["path"]?.jsonPrimitive?.content ?: ""
                val content = jsonElement["content"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Content parameter is required")

                val success = runBlocking { obsidianClient.createFile(path, content) }
                val message = if (success) "File created successfully" else "Failed to create file"

                CallToolResult(
                    content = listOf(TextContent(message))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error creating file: ${e.message}" }
                throw e
            }
        }

        // Update file
        server.addTool(
            name = "mcp_obsidian_update_file",
            description = "Updates an existing file in your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                    ?: throw IllegalArgumentException("Invalid request format")
                val path = jsonElement["path"]?.jsonPrimitive?.content ?: ""
                val content = jsonElement["content"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Content parameter is required")

                val success = runBlocking { obsidianClient.updateFile(path, content) }
                val message = if (success) "File updated successfully" else "Failed to update file"

                CallToolResult(
                    content = listOf(TextContent(message))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error updating file: ${e.message}" }
                throw e
            }
        }

        // Batch get file contents
        server.addTool(
            name = "mcp_obsidian_batch_get_file_contents",
            description = "Gets the content of multiple files in your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                    ?: throw IllegalArgumentException("Invalid request format")
                val filepathsJson = jsonElement["filepaths"]?.jsonArray
                    ?: throw IllegalArgumentException("Filepaths parameter is required")

                val filepaths = filepathsJson.map { it.jsonPrimitive.content }
                val contentMap = runBlocking { obsidianClient.getBatchFileContents(filepaths) }

                val result = buildString {
                    contentMap.forEach { (path, content) ->
                        append("# File: $path\n\n")
                        append(content)
                        append("\n\n")
                    }
                }

                CallToolResult(
                    content = listOf(TextContent(result))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting batch file contents: ${e.message}" }
                throw e
            }
        }

        // Get recent changes
        server.addTool(
            name = "mcp_obsidian_get_recent_changes",
            description = "Gets recently modified files in your Obsidian vault",
            inputSchema = Tool.Input()
        ) { request ->
            try {
                // Extract parameters from JSON
                val jsonElement = request.arguments as? JsonObject
                    ?: JsonObject(emptyMap())
                val limit = jsonElement["limit"]?.jsonPrimitive?.int ?: 10
                val days = jsonElement["days"]?.jsonPrimitive?.int ?: 30

                val recentChanges = runBlocking { obsidianClient.getRecentChanges(limit, days) }
                val result = JsonArray(recentChanges.map { metadata ->
                    JsonObject(
                        mapOf(
                            "path" to JsonPrimitive(metadata.path),
                            "modified" to JsonPrimitive(metadata.modified),
                            "created" to JsonPrimitive(metadata.created)
                        )
                    )
                })

                CallToolResult(
                    content = listOf(TextContent(result.toString()))
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting recent changes: ${e.message}" }
                throw e
            }
        }
    }
}