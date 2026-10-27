package ke.tahadhari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ke.tahadhari.data.store.AndroidStorage
import ke.tahadhari.ui.MobileApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidStorage.init(this) // before AppGraph is used, so the offline cache persists
        setContent { MobileApp() }
    }
}
