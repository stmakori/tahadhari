package ke.tahadhari.platform

import ke.tahadhari.domain.Language
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Desktop speech through the operating system's own synthesizer, with no extra dependency:
 * Windows `System.Speech` (PowerShell), macOS `say`, Linux `espeak-ng` / `espeak`.
 * Text is passed on the process's standard input, never inside the command line.
 */
private enum class Engine { WINDOWS, MAC, ESPEAK }

private val osName = System.getProperty("os.name").orEmpty().lowercase()

private fun onPath(name: String): String? =
    System.getenv("PATH").orEmpty().split(File.pathSeparator).filter { it.isNotBlank() }
        .map { File(it, name) }.firstOrNull { it.isFile && it.canExecute() }?.path

private val espeakPath: String? by lazy { onPath("espeak-ng") ?: onPath("espeak") }

private val engine: Engine? by lazy {
    when {
        "win" in osName -> Engine.WINDOWS
        "mac" in osName -> if (File("/usr/bin/say").canExecute()) Engine.MAC else null
        else -> if (espeakPath != null) Engine.ESPEAK else null
    }
}

/**
 * Fixed script (no text is ever put into it). stdin: line 1 = language code, line 2 = text for that
 * language, line 3 = English fallback. Prints OK or FALLBACK before speaking.
 */
private const val WINDOWS_SCRIPT =
    "[Console]::InputEncoding = [System.Text.Encoding]::UTF8; " +
        "Add-Type -AssemblyName System.Speech; " +
        "\$lang = [Console]::In.ReadLine(); \$text = [Console]::In.ReadLine(); \$fallback = [Console]::In.ReadLine(); " +
        "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
        "\$v = @(\$s.GetInstalledVoices() | Where-Object { \$_.Enabled -and \$_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq \$lang }); " +
        "if (\$v.Count -gt 0) { \$s.SelectVoice(\$v[0].VoiceInfo.Name); [Console]::Out.WriteLine('OK'); [Console]::Out.Flush(); \$s.Speak(\$text) } " +
        "elseif (\$lang -eq 'en') { [Console]::Out.WriteLine('OK'); [Console]::Out.Flush(); \$s.Speak(\$text) } " +
        "else { [Console]::Out.WriteLine('FALLBACK'); [Console]::Out.Flush(); \$s.Speak(\$fallback) }"

private val current = AtomicReference<Process?>(null)

actual fun isSpeechAvailable(): Boolean = engine != null

actual fun speakAloud(text: String, language: Language, englishFallback: String, onEvent: (SpeechEvent) -> Unit) {
    stopSpeaking()
    val which = engine
    if (which == null) { onEvent(SpeechEvent.FAILED); return }
    val sw = language == Language.SWAHILI
    thread(isDaemon = true, name = "tahadhari-speech") {
        try {
            val (command, stdin, fallbackUsed) = when (which) {
                Engine.WINDOWS -> Triple(
                    listOf("powershell", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_SCRIPT),
                    "${if (sw) "sw" else "en"}\n${oneLine(text)}\n${oneLine(englishFallback)}\n", null,
                )
                // No Swahili voice on macOS: say the English text and tell the user.
                Engine.MAC -> Triple(listOf("/usr/bin/say", "-f", "-"), if (sw) englishFallback else text, sw)
                Engine.ESPEAK -> Triple(listOf(espeakPath!!, "-v", if (sw) "sw" else "en", "--stdin"), text, false)
            }
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            current.set(process)
            process.outputStream.use { it.write(stdin.toByteArray(Charsets.UTF_8)) }
            if (which == Engine.WINDOWS) {
                val first = process.inputStream.bufferedReader(Charsets.UTF_8).readLine()
                onEvent(if (first == "FALLBACK") SpeechEvent.STARTED_ENGLISH_FALLBACK else SpeechEvent.STARTED)
            } else {
                onEvent(if (fallbackUsed == true) SpeechEvent.STARTED_ENGLISH_FALLBACK else SpeechEvent.STARTED)
            }
            process.inputStream.readBytes() // drain until the process ends
            val code = process.waitFor()
            val stopped = current.get() !== process // stopSpeaking() replaced or cleared it
            current.compareAndSet(process, null)
            onEvent(if (code == 0 || stopped) SpeechEvent.FINISHED else SpeechEvent.FAILED)
        } catch (_: Exception) {
            current.set(null)
            onEvent(SpeechEvent.FAILED)
        }
    }
}

private fun oneLine(s: String) = s.replace(Regex("\\s+"), " ")

actual fun stopSpeaking() {
    current.getAndSet(null)?.destroyForcibly()
}
