package ke.tahadhari.ai

/** The hosted AI server (Render). Keep in sync with the Android default in composeApp/build.gradle.kts. */
const val HOSTED_PROXY_URL = "https://tahadhari-ai.onrender.com"

/**
 * Base URL of the Tahadhari AI server, or blank to disable AI and use offline text only.
 * Android: tahadhari.proxyUrl in local.properties (default: [HOSTED_PROXY_URL]).
 * Desktop: the TAHADHARI_PROXY_URL environment variable (default: [HOSTED_PROXY_URL]).
 * For a server on your own computer use http://localhost:8080 (desktop) or http://10.0.2.2:8080 (emulator).
 */
expect fun aiProxyUrl(): String

/**
 * Accepts a configured proxy URL only when it is `https://...` or plain-http loopback for development
 * (`http://localhost`, `http://127.0.0.1`). Anything else, including blank, becomes "" (no AI).
 * Used by the web build, where the URL comes from a page meta tag.
 */
fun acceptableProxyUrl(raw: String?): String {
    val url = raw?.trim().orEmpty()
    if (url.isEmpty() || url.any { it.isWhitespace() }) return ""
    if (url.startsWith("https://", ignoreCase = true) && url.length > "https://".length) return url
    val loopback = listOf("http://localhost", "http://127.0.0.1")
    val ok = loopback.any { host ->
        url.startsWith(host, ignoreCase = true) &&
            (url.length == host.length || url[host.length] == ':' || url[host.length] == '/')
    }
    return if (ok) url else ""
}
