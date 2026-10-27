package ke.tahadhari.ai

actual fun aiProxyUrl(): String = System.getenv("TAHADHARI_PROXY_URL") ?: "http://localhost:8080"
