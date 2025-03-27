package com.mcp.server.obsidian

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Client for communicating with the Obsidian Local REST API.
 *
 * @param baseUrl The base URL of the Obsidian REST API. Default is http://localhost:27123.
 * @param apiKey The API key for authenticating with the Obsidian REST API.
 */
class ObsidianClient(
    private val baseUrl: String = "https://localhost:27123",
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

}
