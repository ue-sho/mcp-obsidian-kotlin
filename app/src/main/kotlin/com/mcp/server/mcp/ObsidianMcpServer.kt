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
            name = "list_files_in_vault",
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
    }
}