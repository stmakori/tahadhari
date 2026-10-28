package ke.tahadhari.ai

import kotlinx.browser.document

/**
 * The web build reads the AI server URL from `<meta name="tahadhari-proxy-url" content="...">` in
 * index.html (filled by the Pages workflow from the TAHADHARI_PROXY_URL repository variable).
 * Blank, missing or not https (loopback http allowed for dev) means no AI: template text only.
 */
actual fun aiProxyUrl(): String =
    acceptableProxyUrl(document.querySelector("meta[name=\"tahadhari-proxy-url\"]")?.getAttribute("content"))
