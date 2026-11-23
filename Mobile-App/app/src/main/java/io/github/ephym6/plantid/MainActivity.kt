package io.github.ephym6.plantid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.ephym6.plantid.ui.PlantIdApp
import io.github.ephym6.plantid.ui.theme.PlantIDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantIDTheme {
                PlantIdApp()
            }
        }
    }
}
