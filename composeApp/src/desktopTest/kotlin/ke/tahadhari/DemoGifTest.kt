package ke.tahadhari

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import ke.tahadhari.ui.*
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.FileExportResult
import ke.tahadhari.ui.dashboard.StatusFilter
import ke.tahadhari.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import kotlin.test.Test

/**
 * Renders the README walkthrough GIF (`composeApp/build/demo.gif`) from real composables with
 * sample data. No network, no extra dependencies: frames come from [ImageComposeScene], the GIF
 * from the JDK's `javax.imageio` writer with a small median-cut palette per frame.
 * Skipped unless TAHADHARI_GIF=1:
 *   TAHADHARI_GIF=1 ./gradlew :composeApp:desktopTest --tests '*DemoGifTest*'
 * Individual frames are also saved as PNG in `composeApp/build/demo-frames/`.
 */
class DemoGifTest {
    private val enabled = System.getenv("TAHADHARI_GIF") == "1"

    private val canvasW = 960
    private val canvasH = 600
    private val captionH = 40
    private val bodyH = canvasH - captionH // 560

    private class Frame(val caption: String, val image: BufferedImage)

    private fun render(wDp: Int, hDp: Int, density: Float, content: @Composable () -> Unit): BufferedImage =
        ImageComposeScene((wDp * density).toInt(), (hDp * density).toInt(), Density(density)) { content() }.use { scene ->
            var t = 0L
            repeat(30) { scene.render(t); t += 50_000_000L } // let animations settle (1.5 s)
            val bytes = scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes
            ImageIO.read(ByteArrayInputStream(bytes))
        }

    /** A phone-sized screen (360x640 dp) rendered at the density that makes it 560 px tall. */
    private fun phone(caption: String, content: @Composable () -> Unit): Frame =
        Frame(caption, render(360, 640, bodyH / 640f, content))

    private fun dashboard(caption: String, state: DashboardState): Frame =
        Frame(caption, render(1500, 900, bodyH / 900f) {
            DashboardScreen(state, false, explainer = TemplateExplainer(), onExport = { FileExportResult.Cancelled })
        })

    /** App-like detail screen: header + the real county content, with sample forecast data. */
    private fun detail(lang: Language, scrollPx: Int = 0): @Composable () -> Unit = {
        val county = Counties.byCode(4)!! // Tana River
        val mm = listOf(4.0, 12.0, 28.0, 63.0, 41.0, 9.0, 0.5)
        val days = mm.indices.map { DailyRain("2026-11-0${2 + it}", mm[it], listOf(35, 55, 80, 95, 85, 40, 15)[it]) }
        val forecast = CountyForecast(county, days, RiverOutlook("Tana River", 340.0, 812.0, "2026-11-06"), "2026-11-02")
        val assessment = RiskEngine().assess(forecast)
        val strings = Strings.of(lang)
        TahadhariTheme(false) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        AppMark(Modifier.size(32.dp))
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text(strings.appName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(strings.countyLabel(county.name), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState(scrollPx)).padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        CountyReadyContent(county, forecast, assessment, lang, TemplateExplainer())
                        Disclaimer(strings)
                    }
                }
            }
        }
    }

    private fun sampleDashboard(language: Language = Language.ENGLISH): DashboardState {
        val now = System.currentTimeMillis()
        val severe = setOf(4, 7, 40)
        val high = setOf(3, 5, 41, 42, 43)
        val moderate = setOf(1, 2, 6, 12, 13, 14, 15, 17, 37, 38, 39, 44, 46, 20)
        val state = DashboardState(CoroutineScope(Dispatchers.Unconfined), loader = { error("no network in tests") }, initialLanguage = language)
        Counties.all.forEach { c ->
            val mm = when (c.code) {
                in severe -> Rain.severe
                in high -> Rain.high
                in moderate -> Rain.moderate
                else -> Rain.low
            }
            val river = when (c.code) {
                4, 7 -> RiverOutlook("Tana River", 310.0, 1_020.0, "2026-11-06")
                40, 41 -> RiverOutlook("Nzoia River", 140.0, 330.0, "2026-11-05")
                else -> null
            }
            state.states[c.code] = readyState(c, mm, river, now - 25 * 60_000L)
        }
        return state
    }

    @Test
    fun demoGif() {
        if (!enabled) return
        val frames = listOf(
            phone("1  First launch: what Tahadhari does") {
                TahadhariTheme(false) { OnboardingScreen(Language.ENGLISH, {}, {}) }
            },
            phone("2  Pick your county (all 47)") { MobileApp(initialLanguage = Language.ENGLISH, darkTheme = false) },
            phone("3  Tana River: risk level in plain English", detail(Language.ENGLISH)),
            phone("4  7-day rain chart and river outlook", detail(Language.ENGLISH, scrollPx = 520)),
            phone("5  Same forecast in Kiswahili", detail(Language.SWAHILI)),
            phone("6  Offline safety guides") {
                MobileApp(initialLanguage = Language.ENGLISH, darkTheme = false, initialTab = MobileTab.SAFETY)
            },
            phone("7  Flood guide: step-by-step checklist") {
                MobileApp(initialLanguage = Language.ENGLISH, darkTheme = false, initialTab = MobileTab.SAFETY, initialGuideId = "floods")
            },
            phone("8  Emergency numbers and flood-prone areas") {
                MobileApp(initialCountyCode = 4, initialLanguage = Language.ENGLISH, darkTheme = false, initialTab = MobileTab.EMERGENCY)
            },
            dashboard("9  Desktop dashboard: all 47 counties on a map", sampleDashboard().apply { selectedCode = 4 }),
            dashboard("10  Dashboard filtered to High risk", sampleDashboard().apply {
                selectedCode = 42
                filter = StatusFilter.Level(RiskLevel.HIGH)
            }),
        )
        val canvases = frames.map(::compose)
        val frameDir = File("build/demo-frames").apply { mkdirs() }
        canvases.forEachIndexed { i, img -> ImageIO.write(img, "png", File(frameDir, "frame-%02d.png".format(i + 1))) }
        writeGif(canvases, File("build/demo.gif"), delayMs = 1600)
        println("demo.gif: ${File("build/demo.gif").length() / 1024} KB, ${canvases.size} frames")
    }

    /** Caption bar on top, frame centred below on a neutral background (letterboxing). */
    private fun compose(f: Frame): BufferedImage {
        val out = BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(0x0E, 0x2A, 0x3A)
        g.fillRect(0, 0, canvasW, canvasH)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, 20)
        g.drawString(f.caption, 20, 27)
        val x = (canvasW - f.image.width) / 2
        g.drawImage(f.image, x, captionH, null)
        g.dispose()
        return out
    }

    // ---- GIF encoding -------------------------------------------------------------------------

    private fun writeGif(images: List<BufferedImage>, file: File, delayMs: Int) {
        file.delete()
        val writer = ImageIO.getImageWritersBySuffix("gif").next()
        FileImageOutputStream(file).use { out ->
            writer.output = out
            writer.prepareWriteSequence(null)
            images.forEachIndexed { i, img ->
                val indexed = quantize(img)
                val meta = writer.getDefaultImageMetadata(ImageTypeSpecifier(indexed), null)
                val root = meta.getAsTree(meta.nativeMetadataFormatName) as IIOMetadataNode
                root.child("GraphicControlExtension").apply {
                    setAttribute("disposalMethod", "none")
                    setAttribute("userInputFlag", "FALSE")
                    setAttribute("transparentColorFlag", "FALSE")
                    setAttribute("delayTime", (delayMs / 10).toString())
                    setAttribute("transparentColorIndex", "0")
                }
                if (i == 0) {
                    val app = IIOMetadataNode("ApplicationExtension").apply {
                        setAttribute("applicationID", "NETSCAPE")
                        setAttribute("authenticationCode", "2.0")
                        userObject = byteArrayOf(1, 0, 0) // loop forever
                    }
                    root.child("ApplicationExtensions").appendChild(app)
                }
                meta.setFromTree(meta.nativeMetadataFormatName, root)
                writer.writeToSequence(IIOImage(indexed, null, meta), null)
            }
            writer.endWriteSequence()
        }
        writer.dispose()
    }

    private fun IIOMetadataNode.child(name: String): IIOMetadataNode {
        for (i in 0 until length) if (item(i).nodeName.equals(name, ignoreCase = true)) return item(i) as IIOMetadataNode
        return IIOMetadataNode(name).also { appendChild(it) }
    }

    /** Median-cut to 256 colours over a 5-bit-per-channel histogram. No dithering (UI art is flat colour). */
    private fun quantize(src: BufferedImage): BufferedImage {
        val w = src.width
        val h = src.height
        val px = src.getRGB(0, 0, w, h, null, 0, w)
        val count = IntArray(32768)
        val sumR = LongArray(32768); val sumG = LongArray(32768); val sumB = LongArray(32768)
        for (p in px) {
            val r = p shr 16 and 255; val g = p shr 8 and 255; val b = p and 255
            val k = (r shr 3 shl 10) or (g shr 3 shl 5) or (b shr 3)
            count[k]++; sumR[k] += r.toLong(); sumG[k] += g.toLong(); sumB[k] += b.toLong()
        }
        fun ch(k: Int, c: Int) = when (c) { 0 -> k shr 10 and 31; 1 -> k shr 5 and 31; else -> k and 31 }
        var boxes = mutableListOf(count.indices.filter { count[it] > 0 })
        while (boxes.size < 256) {
            // Split the box with the largest pixel count that still has more than one colour.
            val idx = boxes.indices.filter { boxes[it].size > 1 }.maxByOrNull { i -> boxes[i].sumOf { count[it].toLong() } } ?: break
            val box = boxes[idx]
            val c = (0..2).maxByOrNull { c -> box.maxOf { ch(it, c) } - box.minOf { ch(it, c) } }!!
            val sorted = box.sortedBy { ch(it, c) }
            val half = sorted.sumOf { count[it].toLong() } / 2
            var acc = 0L
            var cut = 1
            for ((i, k) in sorted.withIndex()) { acc += count[k]; if (acc >= half) { cut = (i + 1).coerceIn(1, sorted.size - 1); break } }
            boxes.removeAt(idx)
            boxes.add(sorted.subList(0, cut)); boxes.add(sorted.subList(cut, sorted.size))
        }
        val n = boxes.size
        val rr = ByteArray(256); val gg = ByteArray(256); val bb = ByteArray(256)
        val lookup = IntArray(32768)
        boxes.forEachIndexed { i, box ->
            var c = 0L; var r = 0L; var g = 0L; var b = 0L
            for (k in box) { c += count[k]; r += sumR[k]; g += sumG[k]; b += sumB[k]; lookup[k] = i }
            rr[i] = (r / c).toByte(); gg[i] = (g / c).toByte(); bb[i] = (b / c).toByte()
        }
        val out = BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, IndexColorModel(8, maxOf(n, 2), rr, gg, bb))
        val raster = out.raster
        val row = IntArray(w)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = px[y * w + x]
                row[x] = lookup[((p shr 19 and 31) shl 10) or ((p shr 11 and 31) shl 5) or (p shr 3 and 31)]
            }
            raster.setSamples(0, y, w, 1, 0, row)
        }
        return out
    }
}
