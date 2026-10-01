package ar.trenar.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import ar.trenar.app.notifications.NotificationHelper
import ar.trenar.app.ui.nav.TrenNavHost
import ar.trenar.app.ui.theme.TrenTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val startStationId = intent?.getIntExtra(EXTRA_STATION_ID, -1)
            ?.takeIf { it > 0 }

        setContent {
            TrenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission(),
                        ) { /* result ignored; app works without it */ }
                        LaunchedEffect(Unit) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                android.Manifest.permission.POST_NOTIFICATIONS,
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    TrenNavHost(
                        navController = navController,
                        startStationId = startStationId,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_STATION_ID = "extra_station_id"
    }
}
