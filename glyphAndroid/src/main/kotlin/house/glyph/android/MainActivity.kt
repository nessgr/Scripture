package house.glyph.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import house.glyph.GlyphApp
import house.glyph.data.GlyphAndroidApp

// The entire Android "app": open the window and show the shared UI.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The persisted game's file storage needs the application context.
        GlyphAndroidApp.context = applicationContext
        enableEdgeToEdge()
        setContent {
            GlyphApp()
        }
    }
}