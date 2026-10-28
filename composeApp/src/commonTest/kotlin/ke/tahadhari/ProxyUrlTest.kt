package ke.tahadhari

import ke.tahadhari.ai.acceptableProxyUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyUrlTest {
    @Test fun acceptsHttpsAndLoopbackOnly() {
        assertEquals("https://tahadhari.example.com", acceptableProxyUrl(" https://tahadhari.example.com "))
        assertEquals("http://localhost:8080", acceptableProxyUrl("http://localhost:8080"))
        assertEquals("http://127.0.0.1:8080/x", acceptableProxyUrl("http://127.0.0.1:8080/x"))
        assertEquals("http://localhost", acceptableProxyUrl("http://localhost"))
    }

    @Test fun rejectsEverythingElse() {
        for (bad in listOf(null, "", "  ", "http://example.com", "http://localhost.evil.com", "ftp://x.com",
            "javascript:alert(1)", "https://", "https://a b.com", "example.com")) {
            assertEquals("", acceptableProxyUrl(bad), "should reject: $bad")
        }
    }
}
