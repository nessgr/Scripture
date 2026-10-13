package house.level.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import house.level.LevelApp
import house.level.data.LevelAndroidApp

// The entire Android "app": open the window and show the shared UI.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The tilt source's SensorManager needs the application context.
        LevelAndroidApp.context = applicationContext
        enableEdgeToEdge()
        setContent {
            LevelApp()
        }
    }
}