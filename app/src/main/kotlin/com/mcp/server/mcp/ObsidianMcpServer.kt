package com.mcp.server.mcp

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.*
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
    private val obsidianUrl: String = "https://localhost:27123"
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
        server.addPrompt(
            name = "Kotlin Developer",
            description = "Develop small kotlin applications",
            arguments = listOf(
                PromptArgument(
                    name = "Project Name",
                    description = "Project name for the new project",
                    required = true
                )
            )
        ) { request ->
            GetPromptResult(
                "Description for ${request.name}",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>")
                    )
                )
            )
        }

        // Add a tool
        server.addTool(
            name = "kotlin-sdk-tool",
            description = "A test tool",
            inputSchema = Tool.Input()
        ) { request ->
            CallToolResult(
                content = listOf(TextContent("Hello, world!"))
            )
        }

        // Add a resource
        server.addResource(
            uri = "https://search.com/",
            name = "Web Search",
            description = "Web search engine",
            mimeType = "text/html"
        ) { request ->
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents("Placeholder content for ${request.uri}", request.uri, "text/html")
                )
            )
        }
    }
}