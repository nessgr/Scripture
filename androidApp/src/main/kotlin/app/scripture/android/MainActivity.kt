package app.scripture.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.scripture.App
import app.scripture.data.AndroidApp

// The entire Android "app": register the context (for file storage),
// then open the window and show the shared UI.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidApp.context = applicationContext
        setContent {
            App()
        }
    }
}