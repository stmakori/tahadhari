package ke.tahadhari

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ke.tahadhari.ui.DashboardApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tahadhari · Kenya flood risk dashboard",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        DashboardApp()
    }
}
