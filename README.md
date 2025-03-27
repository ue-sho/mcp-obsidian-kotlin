# MCP Obsidian Kotlin

A Model Context Protocol (MCP) server implementation for Obsidian, written in Kotlin. This project uses the [Obsidian Local REST API](https://github.com/coddingtonbear/obsidian-local-rest-api) plugin to allow AI agents to interact with Obsidian notes.

This project is inspired by [MCP Obsidian](https://github.com/MarkusPfundstein/mcp-obsidian) (Python implementation) but reimplemented in Kotlin.

## Prerequisites

- Java
- [Obsidian](https://obsidian.md/)
- [Local REST API](https://github.com/coddingtonbear/obsidian-local-rest-api) plugin for Obsidian

## Setup

1. Install the Local REST API plugin in Obsidian and configure an API key
2. Clone this repository:
   ```
   git clone https://github.com/your-username/mcp-obsidian-kotlin.git
   cd mcp-obsidian-kotlin
   ```

3. Build the project:
   ```
   ./gradlew build
   ```
4. Set the MCP server in the `mcp.json` file:
   ```json
   {
      "mcpServers": {
         "obsidian": {
               "command": "java",
               "args": [
                  "-jar",
                  "your-path/mcp-obsidian-kotlin/app/build/libs/app.jar"
               ]
         }
      }
   }
   ```
