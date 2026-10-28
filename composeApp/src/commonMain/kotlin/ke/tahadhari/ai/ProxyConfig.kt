package ke.tahadhari.ai

/**
 * Base URL of the Tahadhari AI server, or blank to disable AI and use offline text only.
 * Android: set tahadhari.proxyUrl in local.properties (default: emulator -> your computer).
 * Desktop: set the TAHADHARI_PROXY_URL environment variable (default: http://localhost:8080).
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
