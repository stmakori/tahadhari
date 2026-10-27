package ke.tahadhari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ke.tahadhari.data.store.AndroidStorage
import ke.tahadhari.platform.AndroidPlatform
import ke.tahadhari.ui.MobileRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidStorage.init(this) // before AppGraph is used, so the offline cache and preferences persist
        AndroidPlatform.init(this) // dialer and share sheet intents
        setContent { MobileRoot() }
    }
}
