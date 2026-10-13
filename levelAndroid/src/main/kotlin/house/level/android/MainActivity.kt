package house.level.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import house.level.LevelApp

// The entire Android "app": open the window and show the shared UI.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars (forced from targetSdk 37 anyway);
        // the scaffold's safeDrawingPadding keeps content clear of them.
        enableEdgeToEdge()
        setContent {
            LevelApp()
        }
    }
}