package ke.tahadhari

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import ke.tahadhari.ui.DashboardApp
import ke.tahadhari.ui.MobileRoot
import ke.tahadhari.ui.dashboard.FileExportRequest
import ke.tahadhari.ui.dashboard.FileExportResult
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

/** Web build: the dashboard, or the phone UI when the URL has `?mobile`. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val mobile = window.location.search.removePrefix("?").split('&').any { it == "mobile" || it.startsWith("mobile=") }
    ComposeViewport(document.body!!) {
        if (mobile) MobileRoot() else DashboardApp(onExport = { download(it) })
    }
}

private fun makeBlob(text: String, mime: String): Blob = js("new Blob([text], { type: mime })")

/** Downloads the export through a Blob and a temporary `<a download>` link. */
private fun download(request: FileExportRequest): FileExportResult = try {
    val mime = if (request.extension == "csv") "text/csv;charset=utf-8" else "text/html;charset=utf-8"
    val blob = makeBlob((if (request.utf8Bom) "﻿" else "") + request.content, mime)
    val url = URL.createObjectURL(blob)
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = url
    link.download = request.suggestedFileName
    document.body!!.appendChild(link)
    link.click()
    document.body!!.removeChild(link)
    URL.revokeObjectURL(url)
    FileExportResult.Saved(request.suggestedFileName)
} catch (e: Throwable) {
    FileExportResult.Failed(e.message ?: "error")
}
