/*
 * This source file was generated by the Gradle 'init' task
 */
package com.mcp.server

import mu.KotlinLogging
import com.mcp.server.mcp.ObsidianMcpServer
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}


fun main() {
    try {
        val apiKey = System.getenv("OBSIDIAN_API_KEY")
            ?: throw IllegalStateException("OBSIDIAN_API_KEY environment variable must be set")
        val obsidianUrl = System.getenv("OBSIDIAN_URL") ?: "http://localhost:27123"

        logger.info { "Connecting to Obsidian at $obsidianUrl" }

        val server = ObsidianMcpServer(apiKey, obsidianUrl)
        server.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down mcp-obsidian-kotlin" }
        })
        Thread.currentThread().join()
    } catch (e: Exception) {
        logger.error(e) { "Unhandled exception in main thread" }
        exitProcess(1)
    }
}
