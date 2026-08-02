package ru.tomilo.lib.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.tomilo.lib.mobile.ui.navigation.TomiloNavHost
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TomiloApp
        setContent {
            TomiloTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TomiloNavHost(container = app.container)
                }
            }
        }
    }
}
