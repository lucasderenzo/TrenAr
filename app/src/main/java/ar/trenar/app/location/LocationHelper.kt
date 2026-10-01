package ar.trenar.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationHelper(context: Context) {

    private val appContext = context.applicationContext
    private val fused = LocationServices.getFusedLocationProviderClient(appContext)

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun current(): Location? {
        if (!hasPermission()) return null
        // Never let a stuck GPS fix hang the UI (tunnels, no signal, emulator quirks).
        return withTimeoutOrNull(8_000) {
            try {
                fused.lastLocation.await()
                    ?: fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            } catch (e: Exception) {
                runCatching { fused.lastLocation.await() }.getOrNull()
            }
        }
    }
}
