package ke.tahadhari.ai

/**
 * Base URL of the Tahadhari AI server, or blank to disable AI and use offline text only.
 * Android: set tahadhari.proxyUrl in local.properties (default: emulator -> your computer).
 * Desktop: set the TAHADHARI_PROXY_URL environment variable (default: http://localhost:8080).
 */
expect fun aiProxyUrl(): String
