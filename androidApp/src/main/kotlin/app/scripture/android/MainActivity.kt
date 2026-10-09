package app.scripture.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.scripture.App
import app.scripture.data.AndroidApp

// The entire Android "app": register the context (for file storage), then
// open the window and show the shared UI.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidApp.context = applicationContext
        // Draw behind the system bars (forced from targetSdk 37 anyway);
        // the shared scaffold's safeDrawingPadding keeps content clear of them.
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}