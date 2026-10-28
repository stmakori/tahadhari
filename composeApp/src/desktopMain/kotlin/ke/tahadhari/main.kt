package ke.tahadhari

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ke.tahadhari.ui.DashboardApp
import ke.tahadhari.ui.MobileRoot
import ke.tahadhari.ui.dashboard.FileExportRequest
import ke.tahadhari.ui.dashboard.FileExportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Desktop dashboard. Pass `--mobile` to preview the phone UI in a phone-sized window. */
fun main(args: Array<String>) = application {
    val mobile = "--mobile" in args
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tahadhari · Kenya flood risk dashboard",
        state = rememberWindowState(width = if (mobile) 412.dp else 1480.dp, height = if (mobile) 860.dp else 900.dp),
    ) {
        if (mobile) {
            MobileRoot()
        } else {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(960, 640) }
            DashboardApp(onExport = { saveFile(window, it) })
        }
    }
}

/**
 * Asks where to save the exported file (CSV or HTML briefing) with the native "Save" dialog on the
 * AWT thread, then writes it as UTF-8, with a byte-order mark when the request asks for one
 * (spreadsheet apps need it to read names such as Murang'a correctly).
 */
private suspend fun saveFile(owner: Frame, request: FileExportRequest): FileExportResult {
    val suffix = "." + request.extension
    val target = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(owner, request.dialogTitle, FileDialog.SAVE).apply {
            file = request.suggestedFileName
            setFilenameFilter { _, name -> name.endsWith(suffix, ignoreCase = true) }
            isVisible = true // modal: returns when the user picks a file or cancels
        }
        val dir = dialog.directory
        val name = dialog.file
        if (dir == null || name == null) null
        else File(dir, if (name.endsWith(suffix, ignoreCase = true)) name else name + suffix)
    } ?: return FileExportResult.Cancelled
    return try {
        withContext(Dispatchers.IO) {
            target.writeText((if (request.utf8Bom) "\uFEFF" else "") + request.content, Charsets.UTF_8)
        }
        FileExportResult.Saved(target.name)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FileExportResult.Failed(e.message ?: e::class.simpleName ?: "error")
    }
}
