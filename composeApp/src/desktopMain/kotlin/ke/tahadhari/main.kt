package ke.tahadhari

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ke.tahadhari.ui.DashboardApp
import ke.tahadhari.ui.MobileApp

/** Desktop dashboard. Pass `--mobile` to preview the phone UI in a phone-sized window. */
fun main(args: Array<String>) = application {
    val mobile = "--mobile" in args
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tahadhari · Kenya flood risk dashboard",
        state = rememberWindowState(width = if (mobile) 412.dp else 1200.dp, height = if (mobile) 860.dp else 800.dp),
    ) {
        if (mobile) MobileApp() else DashboardApp()
    }
}
