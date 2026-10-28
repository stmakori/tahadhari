package ke.tahadhari.platform

import ke.tahadhari.domain.Language

@JsFun("() => typeof window !== 'undefined' && 'speechSynthesis' in window && typeof SpeechSynthesisUtterance !== 'undefined'")
private external fun jsSpeechSupported(): Boolean

/**
 * Speaks with the browser's speechSynthesis. Returns 0 = started in the requested language,
 * 1 = no voice for it, started the fallback text, 2 = failed to start. [onEnd] runs when speech ends or errors.
 */
@JsFun(
    "(text, fallback, lang, onEnd) => { try {" +
        "const synth = window.speechSynthesis; synth.cancel();" +
        "const voices = synth.getVoices();" +
        "const voice = voices.find(v => v.lang && v.lang.toLowerCase().startsWith(lang));" +
        "const useFallback = lang !== 'en' && !voice;" +
        "const u = new SpeechSynthesisUtterance(useFallback ? fallback : text);" +
        "const v = useFallback ? voices.find(x => x.lang && x.lang.toLowerCase().startsWith('en')) : voice;" +
        "if (v) { u.voice = v; u.lang = v.lang; } else { u.lang = useFallback ? 'en' : lang; }" +
        "u.onend = () => onEnd(); u.onerror = () => onEnd();" +
        "synth.speak(u); return useFallback ? 1 : 0; } catch (e) { return 2; } }"
)
private external fun jsSpeak(text: String, fallback: String, lang: String, onEnd: () -> Unit): Int

@JsFun("() => { try { window.speechSynthesis.cancel(); } catch (e) {} }")
private external fun jsStop()

actual fun isSpeechAvailable(): Boolean = try { jsSpeechSupported() } catch (_: Throwable) { false }

actual fun speakAloud(text: String, language: Language, englishFallback: String, onEvent: (SpeechEvent) -> Unit) {
    val code = try {
        jsSpeak(text, englishFallback, if (language == Language.SWAHILI) "sw" else "en") { onEvent(SpeechEvent.FINISHED) }
    } catch (_: Throwable) { 2 }
    when (code) {
        0 -> onEvent(SpeechEvent.STARTED)
        1 -> onEvent(SpeechEvent.STARTED_ENGLISH_FALLBACK)
        else -> onEvent(SpeechEvent.FAILED)
    }
}

actual fun stopSpeaking() {
    try { jsStop() } catch (_: Throwable) {}
}
