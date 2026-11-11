package ke.tahadhari.server

/** Origins allowed to call the server from a browser when TAHADHARI_ALLOWED_ORIGINS is not set (wasm dev server). */
val DEFAULT_ALLOWED_ORIGINS: List<String> = listOf("http://localhost:8080", "http://127.0.0.1:8080")

const val ANTHROPIC_BASE_URL = "https://api.anthropic.com"

private val AWS_REGION = Regex("^[a-z]{2}(-[a-z]+)+-\\d$")
private val WORKSPACE_ID = Regex("^wrkspc_[A-Za-z0-9]+$")

/**
 * Where the Messages API lives. The Claude API takes the key in `x-api-key`. Claude Platform on AWS
 * (Anthropic-operated, billed through AWS) has a regional address, takes the key from the AWS console
 * as `Authorization: Bearer`, and needs the workspace ID on every request. Model IDs are the same.
 */
data class ClaudeEndpoint(val baseUrl: String, val awsWorkspaceId: String? = null) {
    companion object {
        val CLAUDE_API = ClaudeEndpoint(ANTHROPIC_BASE_URL)

        fun awsPlatform(region: String, workspaceId: String): ClaudeEndpoint {
            require(AWS_REGION.matches(region)) { "AWS_REGION must look like eu-central-1" }
            require(WORKSPACE_ID.matches(workspaceId)) { "ANTHROPIC_AWS_WORKSPACE_ID must look like wrkspc_..." }
            return ClaudeEndpoint("https://aws-external-anthropic.$region.api.aws", workspaceId)
        }
    }
}

data class ServerConfig(
    val apiKey: String,
    val model: String,
    val port: Int,
    val requestsPerHour: Int,
    val allowedOrigins: List<String> = DEFAULT_ALLOWED_ORIGINS,
    /** Proxies in front of the server that append to X-Forwarded-For (0 = use the socket address). */
    val trustedProxyHops: Int = 0,
    val endpoint: ClaudeEndpoint = ClaudeEndpoint.CLAUDE_API,
) {
    companion object {
        /**
         * Claude API: ANTHROPIC_API_KEY. Claude Platform on AWS: also set ANTHROPIC_AWS_WORKSPACE_ID and
         * AWS_REGION; the key comes from ANTHROPIC_AWS_API_KEY, or ANTHROPIC_API_KEY so one secret works.
         */
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig {
            // Values pasted into a host dashboard often carry a trailing space or newline (a 401 for keys).
            fun value(name: String) = env[name]?.trim()?.takeIf { it.isNotEmpty() }
            val workspaceId = value("ANTHROPIC_AWS_WORKSPACE_ID")
            val endpoint = if (workspaceId == null) ClaudeEndpoint.CLAUDE_API else ClaudeEndpoint.awsPlatform(
                region = value("AWS_REGION") ?: error("Set AWS_REGION to the region of your Claude Platform on AWS workspace"),
                workspaceId = workspaceId,
            )
            val apiKey = (if (workspaceId != null) value("ANTHROPIC_AWS_API_KEY") else null)
                ?: value("ANTHROPIC_API_KEY")
                ?: error("Set the ANTHROPIC_API_KEY environment variable")
            return ServerConfig(
                apiKey = apiKey,
                model = value("TAHADHARI_MODEL") ?: "claude-haiku-5-5",
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                requestsPerHour = env["TAHADHARI_REQUESTS_PER_HOUR"]?.toIntOrNull() ?: 60,
                allowedOrigins = parseAllowedOrigins(env["TAHADHARI_ALLOWED_ORIGINS"]),
                trustedProxyHops = env["TAHADHARI_TRUSTED_PROXY_HOPS"]?.toIntOrNull()?.coerceIn(0, 5) ?: 0,
                endpoint = endpoint,
            )
        }

        /** Comma-separated full origins ("https://user.github.io"). Blank or missing gives the dev default. */
        fun parseAllowedOrigins(raw: String?): List<String> {
            val parsed = raw.orEmpty().split(',').map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
            return parsed.ifEmpty { DEFAULT_ALLOWED_ORIGINS }
        }
    }
}
