package ke.tahadhari.ui.dashboard

/**
 * What the dashboard hands the platform to save: file [content], suggested name, dialog title and
 * the file [extension] (without dot, e.g. "csv" or "html"). [utf8Bom] adds a byte-order mark,
 * which spreadsheet apps need to read names such as Murang'a correctly; HTML does not need it.
 */
data class FileExportRequest(
    val content: String,
    val suggestedFileName: String,
    val dialogTitle: String,
    val extension: String,
    val utf8Bom: Boolean = false,
)

/** Outcome of saving, shown as a snackbar (nothing for [Cancelled]). */
sealed interface FileExportResult {
    data class Saved(val fileName: String) : FileExportResult
    data object Cancelled : FileExportResult
    data class Failed(val message: String) : FileExportResult
}
