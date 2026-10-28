package ke.tahadhari

import android.app.Application
import ke.tahadhari.platform.AndroidPlatform
import ke.tahadhari.platform.installCrashHandler

/**
 * Runs before any activity, receiver or worker in this process, so the local crash log also
 * covers crashes in the widget and the background refresh.
 */
class TahadhariApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.init(this)
        installCrashHandler()
    }
}
