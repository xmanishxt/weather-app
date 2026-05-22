package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NotificationAlert
import com.example.data.local.SavedLocation
import com.example.data.remote.GeocodingResult
import com.example.data.remote.WeatherResponse
import com.example.data.repository.WeatherRepository
import com.example.ui.components.NotificationHelper
import com.example.ui.components.WeatherHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val response: WeatherResponse,
        val insight: String,
        val activeLocation: SavedLocation
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface SearchState {
    object Idle : SearchState
    object Searching : SearchState
    data class Results(val results: List<GeocodingResult>) : SearchState
    data class Error(val message: String) : SearchState
}

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    val savedLocations: StateFlow<List<SavedLocation>> = repository.savedLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notificationAlerts: StateFlow<List<NotificationAlert>> = repository.notificationAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alert settings (stored in state for reactive UI updates)
    val severeAlertsEnabled = MutableStateFlow(true)
    val rainAlertsEnabled = MutableStateFlow(true)
    val dailySummaryEnabled = MutableStateFlow(false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Prepare notification channel
        NotificationHelper.createNotificationChannel(context)

        // Seed default location if empty and load
        viewModelScope.launch {
            repository.savedLocations.first().let { currentList ->
                if (currentList.isEmpty()) {
                    // Seed Reykjavik (ambient polar vibe) and Tokyo
                    val defaultLoc = SavedLocation(
                        name = "Reykjavik",
                        latitude = 64.1466,
                        longitude = -21.9426,
                        country = "Iceland",
                        state = "Hofuoborgarsveoiuo",
                        isCurrent = true
                    )
                    repository.insertLocation(defaultLoc)
                    repository.insertLocation(SavedLocation(
                        name = "Tokyo",
                        latitude = 35.6762,
                        longitude = 139.6503,
                        country = "Japan",
                        state = "Tokyo",
                        isCurrent = false
                    ))
                    loadForecastForLocation(defaultLoc)
                } else {
                    val active = currentList.firstOrNull { it.isCurrent } ?: currentList.first()
                    loadForecastForLocation(active)
                }
            }
        }
    }

    fun makeLocationActive(location: SavedLocation) {
        viewModelScope.launch {
            repository.makeActiveLocation(location)
            loadForecastForLocation(location)
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            val list = savedLocations.value
            val target = list.firstOrNull { it.id == id }
            repository.deleteLocationById(id)
            if (target?.isCurrent == true) {
                // Find next location to make active
                val remaining = list.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    makeLocationActive(remaining.first())
                }
            }
        }
    }

    fun loadForecastForLocation(location: SavedLocation) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            refreshForecastInternal(location)
        }
    }

    fun refreshActiveLocation() {
        viewModelScope.launch {
            val list = savedLocations.value
            val active = list.firstOrNull { it.isCurrent } ?: list.firstOrNull()
            if (active != null) {
                _isRefreshing.value = true
                refreshForecastInternal(active)
                _isRefreshing.value = false
            } else {
                _uiState.value = WeatherUiState.Error("No active location configured.")
            }
        }
    }

    private suspend fun refreshForecastInternal(location: SavedLocation) {
        try {
            val response = withContext(Dispatchers.IO) {
                repository.fetchForecast(location.latitude, location.longitude)
            }
            
            // Build weather summary string for generative model
            val currentCode = response.current?.weatherCode ?: 0
            val weatherDetails = WeatherHelper.getWeatherDetails(currentCode)
            val weatherSummary = "${location.name}: Clear skies/Conditions are ${weatherDetails.description} at ${response.current?.temperature}°C, humidity ${response.current?.humidity}%, wind ${response.current?.windSpeed} km/h."
            
            val insight = withContext(Dispatchers.IO) {
                repository.getWeatherInsight(weatherSummary)
            }

            _uiState.value = WeatherUiState.Success(
                response = response,
                insight = insight,
                activeLocation = location
            )

            // Evaluate automatic real-time alerts depending on conditions and configurations
            evaluateAutoAlerts(response, location)

        } catch (e: Exception) {
            _uiState.value = WeatherUiState.Error("Failed to fetch weather: ${e.localizedMessage}")
        }
    }

    private fun evaluateAutoAlerts(response: WeatherResponse, location: SavedLocation) {
        viewModelScope.launch {
            val current = response.current ?: return@launch
            val code = current.weatherCode

            // 1. Severe Weather Alert (Code >= 65 is severe rain/snow/thunderstorms)
            if (severeAlertsEnabled.value && (code == 65 || code == 96 || code == 99)) {
                val title = "Severe Weather Alert"
                val message = "Heavy hazard conditions in ${location.name}. Active ${WeatherHelper.getWeatherDetails(code).description} detected. Take care when traveling."
                triggerNotification(title, message, "SEVERE")
            }

            // 2. Rain Warning Alert (Precipitation detected)
            if (rainAlertsEnabled.value && (current.rain > 0.0 || current.showers > 0.0)) {
                val title = "Precipitation Warning"
                val message = "It is currently raining in ${location.name} (${current.rain} mm). Perfect time for a stylish jacket and dynamic umbrella."
                triggerNotification(title, message, "RAIN")
            }
        }
    }

    fun searchCity(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchState.Searching
            try {
                val response = withContext(Dispatchers.IO) {
                    repository.searchCity(query)
                }
                val results = response.results
                if (results != null && results.isNotEmpty()) {
                    _searchState.value = SearchState.Results(results)
                } else {
                    _searchState.value = SearchState.Results(emptyList())
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("No cities found: ${e.localizedMessage}")
            }
        }
    }

    fun addSearchedLocation(result: GeocodingResult) {
        viewModelScope.launch {
            val newLoc = SavedLocation(
                name = result.name,
                latitude = result.latitude,
                longitude = result.longitude,
                country = result.country ?: "Unknown",
                state = result.state ?: "",
                isCurrent = false
            )
            repository.insertLocation(newLoc)
            _searchState.value = SearchState.Idle
            makeLocationActive(newLoc)
        }
    }

    fun triggerManualTestNotification() {
        viewModelScope.launch {
            val activeLocName = when (val state = uiState.value) {
                is WeatherUiState.Success -> state.activeLocation.name
                else -> "your area"
            }
            val title = "Instant Real-Time Sync"
            val message = "System telemetry check: Clean minimalist dashboard for $activeLocName synchronized successfully."
            triggerNotification(title, message, "WEATHER_UPDATE")
        }
    }

    private suspend fun triggerNotification(title: String, message: String, type: String) {
        val alert = NotificationAlert(
            title = title,
            message = message,
            type = type
        )
        // Store in local database
        repository.insertAlert(alert)
        // Fire system notification banner
        NotificationHelper.triggerSystemNotification(context, title, message)
    }

    fun clearAlertLogs() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }
}
