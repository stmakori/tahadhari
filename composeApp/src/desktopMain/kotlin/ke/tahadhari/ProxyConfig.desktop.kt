package ke.tahadhari.ai

actual fun aiProxyUrl(): String = System.getenv("TAHADHARI_PROXY_URL") ?: HOSTED_PROXY_URL
