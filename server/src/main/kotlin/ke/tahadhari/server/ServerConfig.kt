package ke.tahadhari.server

data class ServerConfig(
    val apiKey: String,
    val model: String,
    val port: Int,
    val requestsPerHour: Int,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) = ServerConfig(
            apiKey = env["ANTHROPIC_API_KEY"]?.takeIf { it.isNotBlank() }
                ?: error("Set the ANTHROPIC_API_KEY environment variable"),
            model = env["TAHADHARI_MODEL"] ?: "claude-haiku-4-5",
            port = env["PORT"]?.toIntOrNull() ?: 8080,
            requestsPerHour = env["TAHADHARI_REQUESTS_PER_HOUR"]?.toIntOrNull() ?: 60,
        )
    }
}
