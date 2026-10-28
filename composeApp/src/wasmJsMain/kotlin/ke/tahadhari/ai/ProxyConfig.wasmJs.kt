package ke.tahadhari.ai

/** The web build never calls the AI server (it holds a key and is not publicly deployed): template text only. */
actual fun aiProxyUrl(): String = ""
