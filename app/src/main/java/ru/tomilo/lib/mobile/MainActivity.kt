package ru.tomilo.lib.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import ru.tomilo.lib.mobile.ui.navigation.TomiloNavHost
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional: no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val app = application as TomiloApp
        setContent {
            TomiloTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TomiloNavHost(container = app.container)
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
