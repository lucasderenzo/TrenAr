package ar.trenar.app.ui.cercanas

import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.trenar.app.data.model.BikeNearby
import ar.trenar.app.data.model.ManualLocation
import ar.trenar.app.data.model.StationWithNext
import ar.trenar.app.data.remote.NominatimClient
import ar.trenar.app.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class CercanasMode { TREN, ECOBICI }

// Bounding box of the train network (stations.json spans Chascomús–Zárate, Junín–La Plata) plus a margin.
private const val AREA_SOUTH = -35.8
private const val AREA_NORTH = -33.9
private const val AREA_WEST = -61.2
private const val AREA_EAST = -57.7

data class CercanasUiState(
    val mode: CercanasMode = CercanasMode.TREN,
    val items: List<StationWithNext> = emptyList(),
    val bikeItems: List<BikeNearby> = emptyList(),
    val loading: Boolean = false,
    val locationDenied: Boolean = false,
    val locationUnavailable: Boolean = false,
    val radiusMeters: Int = 1000,
    val bikeFavorites: Set<String> = emptySet(),
    /** User's current street address (reverse-geocoded), e.g. "Tres Arroyos 660". */
    val currentAddress: String? = null,
    /** Manually entered address+coords; when set it's used instead of the GPS fix. */
    val manualLocation: ManualLocation? = null,
    /** Where the address being typed in the manual-location dialog resolves to (shown as a pin). */
    val preview: ManualLocation? = null,
    /** True while resolving a manually typed address to coordinates. */
    val geocoding: Boolean = false,
    /** True when the last manual address couldn't be resolved to a location. */
    val geocodeError: Boolean = false,
)

class CercanasViewModel : ViewModel() {

    private val repo = ServiceLocator.repository
    private val location = ServiceLocator.location
    private val prefs = ServiceLocator.prefs

    private val _state = MutableStateFlow(CercanasUiState())
    val state = _state.asStateFlow()

    private var lastLoc: Location? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.nearbyRadiusMeters.collect { r -> _state.update { it.copy(radiusMeters = r) } }
        }
        viewModelScope.launch {
            prefs.bikeFavorites.collect { f -> _state.update { it.copy(bikeFavorites = f) } }
        }
        viewModelScope.launch {
            val manual = prefs.manualLocationNow()
            if (manual != null) {
                _state.update { it.copy(manualLocation = manual, currentAddress = manual.address) }
                load(_state.value.mode, force = true)
            }
        }
    }

    fun refresh() = load(_state.value.mode, force = true)

    fun setMode(mode: CercanasMode) {
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode) }
        val needs = when (mode) {
            CercanasMode.TREN -> _state.value.items.isEmpty()
            CercanasMode.ECOBICI -> _state.value.bikeItems.isEmpty()
        }
        if (needs) load(mode, force = false) else _state.update { it.copy(loading = false) }
    }

    fun setRadius(meters: Int) {
        if (meters == _state.value.radiusMeters) return
        viewModelScope.launch { prefs.setNearbyRadius(meters) }
        _state.update { it.copy(radiusMeters = meters) }
        load(_state.value.mode, force = true)
    }

    fun toggleBikeFavorite(id: String) {
        viewModelScope.launch { prefs.toggleBikeFavorite(id) }
    }

    /** The manual-location dialog opened: start from the address in use, if any. */
    fun startManualEdit() {
        previewJob?.cancel()
        _state.update { it.copy(preview = it.manualLocation, geocoding = false, geocodeError = false) }
    }

    /** The user is typing an address: once they pause, resolve it and show it as a pin. */
    fun previewAddress(text: String) {
        previewJob?.cancel()
        val query = text.trim()
        if (query.length < 4) {
            _state.update { it.copy(preview = null, geocoding = false, geocodeError = false) }
            return
        }
        if (_state.value.preview?.address == query) return
        previewJob = viewModelScope.launch {
            // Wait for a pause in typing; this also keeps Nominatim under its 1 request/s policy.
            delay(900)
            _state.update { it.copy(geocoding = true, geocodeError = false) }
            val coords = geocodeAddress(query)
            _state.update {
                it.copy(
                    geocoding = false,
                    geocodeError = coords == null,
                    preview = coords?.let { (lat, lng) -> ManualLocation(lat, lng, query) },
                )
            }
        }
    }

    /** Uses the typed address instead of the GPS fix; [onSaved] runs once it's resolved and stored. */
    fun confirmManualAddress(text: String, onSaved: () -> Unit) {
        val query = text.trim()
        if (query.isEmpty()) return
        previewJob?.cancel()
        viewModelScope.launch {
            val manual = _state.value.preview?.takeIf { it.address == query } ?: run {
                _state.update { it.copy(geocoding = true, geocodeError = false) }
                geocodeAddress(query)?.let { (lat, lng) -> ManualLocation(lat, lng, query) }
            }
            if (manual == null) {
                _state.update { it.copy(geocoding = false, geocodeError = true, preview = null) }
                return@launch
            }
            prefs.setManualLocation(manual.lat, manual.lng, manual.address)
            _state.update {
                it.copy(
                    geocoding = false,
                    geocodeError = false,
                    preview = null,
                    manualLocation = manual,
                    currentAddress = manual.address,
                )
            }
            onSaved()
            load(_state.value.mode, force = true)
        }
    }

    /** The manual-location dialog was dismissed without confirming. */
    fun cancelManualEdit() {
        previewJob?.cancel()
        _state.update { it.copy(preview = null, geocoding = false, geocodeError = false) }
    }

    /** Drops the manual address and goes back to using the device's real GPS location. */
    fun useCurrentLocation() {
        lastLoc = null
        viewModelScope.launch { prefs.clearManualLocation() }
        _state.update { it.copy(manualLocation = null, currentAddress = null) }
        load(_state.value.mode, force = true)
    }

    /** Best-effort reverse geocode to a "street number" string; null when unavailable/offline. */
    private suspend fun reverseGeocode(loc: Location): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(ServiceLocator.appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            val a = results?.firstOrNull() ?: return@withContext null
            val street = a.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
            val number = a.subThoroughfare?.trim()?.takeIf { it.isNotBlank() }
            when {
                street != null && number != null -> "$street $number"
                street != null -> street
                else -> a.featureName?.takeIf { it.any(Char::isLetter) }
                    ?: a.subLocality ?: a.locality
            }
        }.getOrNull()
    }

    /**
     * Best-effort forward geocode of a typed address to lat/lng; null when not found/offline.
     * Restricted to the area the train network covers, so "Tres Arroyos 660" resolves to the
     * street in CABA and not to the city of Tres Arroyos. Tries the platform Geocoder first and
     * falls back to OpenStreetMap's Nominatim (devices without Google services, emulators).
     */
    private suspend fun geocodeAddress(query: String): Pair<Double, Double>? =
        platformGeocode(query)
            ?: NominatimClient.search(query, AREA_SOUTH, AREA_WEST, AREA_NORTH, AREA_EAST)

    private suspend fun platformGeocode(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(ServiceLocator.appContext, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(
                query, 5, AREA_SOUTH, AREA_WEST, AREA_NORTH, AREA_EAST,
            )
            val a = results?.firstOrNull { it.latitude in AREA_SOUTH..AREA_NORTH && it.longitude in AREA_WEST..AREA_EAST }
                ?: return@withContext null
            a.latitude to a.longitude
        }.getOrNull()
    }

    private fun load(mode: CercanasMode, force: Boolean) {
        viewModelScope.launch {
            val manual = _state.value.manualLocation
            if (manual != null) {
                _state.update { it.copy(loading = true, locationDenied = false, locationUnavailable = false) }
                fetchNearby(mode, manual.lat, manual.lng)
                return@launch
            }
            if (!location.hasPermission()) {
                _state.update { it.copy(locationDenied = true, loading = false) }
                return@launch
            }
            _state.update { it.copy(loading = true, locationDenied = false, locationUnavailable = false) }
            val loc = (if (force) null else lastLoc)
                ?: runCatching { location.current() }
                    .onFailure { android.util.Log.e("TrenCercanas", "location failed", it) }
                    .getOrNull()?.also { lastLoc = it }
            // A manual location may have been set (or restored from prefs) while waiting for the GPS fix.
            if (_state.value.manualLocation != null) return@launch
            if (loc == null) {
                _state.update { it.copy(loading = false, locationUnavailable = true) }
                return@launch
            }
            if (force || _state.value.currentAddress == null) {
                viewModelScope.launch {
                    val addr = reverseGeocode(loc)
                    if (addr != null && _state.value.manualLocation == null) {
                        _state.update { it.copy(currentAddress = addr) }
                    }
                }
            }
            fetchNearby(mode, loc.latitude, loc.longitude)
        }
    }

    private suspend fun fetchNearby(mode: CercanasMode, lat: Double, lng: Double) {
        try {
            val radius = _state.value.radiusMeters
            val noRadius = radius == Int.MAX_VALUE
            when (mode) {
                CercanasMode.TREN -> {
                    // "Sin radio" lists every station; otherwise all within the radius.
                    val listCap = if (noRadius) 400 else 30
                    val near = repo.nearestStationsInRadius(lat, lng, radius, cap = listCap)
                    // fetch live arrivals only for the nearest few; the rest are plain rows.
                    val arrivalsCount = 15
                    _state.update {
                        it.copy(
                            items = near.mapIndexed { i, s -> StationWithNext(s, null, loading = i < arrivalsCount) },
                            loading = false,
                        )
                    }
                    val enriched = repo.withNextArrivals(near.take(arrivalsCount))
                    if (_state.value.mode == CercanasMode.TREN) {
                        val merged = enriched + near.drop(arrivalsCount).map { StationWithNext(it, null) }
                        _state.update { it.copy(items = merged) }
                    }
                }
                CercanasMode.ECOBICI -> {
                    val bikes = repo.nearestBikes(lat, lng, radius, limit = if (noRadius) 400 else 20)
                    _state.update { it.copy(bikeItems = bikes, loading = false) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TrenCercanas", "load failed", e)
            _state.update { it.copy(loading = false, locationUnavailable = true) }
        }
    }
}
