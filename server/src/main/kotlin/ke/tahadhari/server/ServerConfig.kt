package ke.tahadhari.server

/** Origins allowed to call the server from a browser when TAHADHARI_ALLOWED_ORIGINS is not set (wasm dev server). */
val DEFAULT_ALLOWED_ORIGINS: List<String> = listOf("http://localhost:8080", "http://127.0.0.1:8080")

data class ServerConfig(
    val apiKey: String,
    val model: String,
    val port: Int,
    val requestsPerHour: Int,
    val allowedOrigins: List<String> = DEFAULT_ALLOWED_ORIGINS,
    /** Proxies in front of the server that append to X-Forwarded-For (0 = use the socket address). */
    val trustedProxyHops: Int = 0,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) = ServerConfig(
            apiKey = env["ANTHROPIC_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() }
                ?: error("Set the ANTHROPIC_API_KEY environment variable"),
            model = env["TAHADHARI_MODEL"] ?: "claude-haiku-5-5",
            port = env["PORT"]?.toIntOrNull() ?: 8080,
            requestsPerHour = env["TAHADHARI_REQUESTS_PER_HOUR"]?.toIntOrNull() ?: 60,
            allowedOrigins = parseAllowedOrigins(env["TAHADHARI_ALLOWED_ORIGINS"]),
            trustedProxyHops = env["TAHADHARI_TRUSTED_PROXY_HOPS"]?.toIntOrNull()?.coerceIn(0, 5) ?: 0,
        )

        /** Comma-separated full origins ("https://user.github.io"). Blank or missing gives the dev default. */
        fun parseAllowedOrigins(raw: String?): List<String> {
            val parsed = raw.orEmpty().split(',').map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
            return parsed.ifEmpty { DEFAULT_ALLOWED_ORIGINS }
        }
    }
}
