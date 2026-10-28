package ke.tahadhari.platform

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import ke.tahadhari.domain.Language
import java.util.Locale

/** Android text-to-speech. One engine at a time; it is shut down when speech ends, fails or is stopped. */
private object TtsSession {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0

    fun speak(text: String, language: Language, fallback: String, onEvent: (SpeechEvent) -> Unit) {
        val context = AndroidPlatform.appContext
        if (context == null) { onEvent(SpeechEvent.FAILED); return }
        stop()
        val gen = ++generation
        fun report(e: SpeechEvent) = main.post { onEvent(e) }
        fun finish(e: SpeechEvent) = main.post {
            if (gen == generation) { release(); onEvent(e) }
        }
        try {
            var created: TextToSpeech? = null
            created = TextToSpeech(context) { status ->
                main.post {
                    val tts = created
                    if (gen != generation || tts == null) { tts?.shutdown(); return@post }
                    if (status != TextToSpeech.SUCCESS) { finish(SpeechEvent.FAILED); return@post }
                    start(tts, text, language, fallback, gen, ::report, ::finish)
                }
            }
            engine = created
        } catch (_: Exception) {
            release()
            onEvent(SpeechEvent.FAILED)
        }
    }

    private fun start(
        tts: TextToSpeech, text: String, language: Language, fallback: String, gen: Int,
        report: (SpeechEvent) -> Unit, finish: (SpeechEvent) -> Unit,
    ) {
        try {
            val locale = voiceFor(tts, language)
            val useFallback = locale == null
            val spoken = if (useFallback) fallback else text
            tts.setLanguage(locale ?: voiceFor(tts, Language.ENGLISH) ?: Locale.US)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = finish(SpeechEvent.FINISHED)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish(SpeechEvent.FAILED)
                override fun onStop(utteranceId: String?, interrupted: Boolean) {}
            })
            val max = TextToSpeech.getMaxSpeechInputLength() - 1
            val rc = tts.speak(spoken.take(max), TextToSpeech.QUEUE_FLUSH, null, "tahadhari-alert-$gen")
            if (rc != TextToSpeech.SUCCESS) { finish(SpeechEvent.FAILED); return }
            report(if (useFallback) SpeechEvent.STARTED_ENGLISH_FALLBACK else SpeechEvent.STARTED)
        } catch (_: Exception) {
            finish(SpeechEvent.FAILED)
        }
    }

    /** A locale the installed voices support for [language], or null (Swahili: sw-KE, then sw). */
    private fun voiceFor(tts: TextToSpeech, language: Language): Locale? {
        val candidates = if (language == Language.SWAHILI) listOf(Locale("sw", "KE"), Locale("sw"))
        else listOf(Locale("en", "KE"), Locale.ENGLISH)
        return candidates.firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
    }

    fun stop() {
        val had = engine != null
        generation++ // ignore callbacks from the engine being released
        release()
        if (had) return
    }

    private fun release() {
        val e = engine ?: return
        engine = null
        try { e.stop() } catch (_: Exception) {}
        try { e.shutdown() } catch (_: Exception) {}
    }
}

actual fun isSpeechAvailable(): Boolean = true

actual fun speakAloud(text: String, language: Language, englishFallback: String, onEvent: (SpeechEvent) -> Unit) =
    TtsSession.speak(text, language, englishFallback, onEvent)

actual fun stopSpeaking() = TtsSession.stop()
