package com.example.data.repository

import com.example.data.local.NotificationAlert
import com.example.data.local.SavedLocation
import com.example.data.local.WeatherDao
import com.example.data.remote.OpenMeteoClient
import com.example.data.remote.WeatherResponse
import com.example.data.remote.GeocodingResponse
import com.example.data.remote.GeminiWorker
import kotlinx.coroutines.flow.Flow

class WeatherRepository(private val weatherDao: WeatherDao) {

    val savedLocations: Flow<List<SavedLocation>> = weatherDao.getAllLocations()
    val notificationAlerts: Flow<List<NotificationAlert>> = weatherDao.getAllAlerts()

    suspend fun insertLocation(location: SavedLocation) {
        weatherDao.insertLocation(location)
    }

    suspend fun deleteLocationById(id: Int) {
        weatherDao.deleteLocationById(id)
    }

    suspend fun makeActiveLocation(location: SavedLocation) {
        weatherDao.makeActiveLocation(location)
    }

    suspend fun getCurrentActiveLocation(): SavedLocation? {
        return weatherDao.getCurrentLocation()
    }

    suspend fun fetchForecast(latitude: Double, longitude: Double): WeatherResponse {
        return OpenMeteoClient.service.getForecast(latitude, longitude)
    }

    suspend fun searchCity(query: String): GeocodingResponse {
        return OpenMeteoClient.service.searchCity(query)
    }

    suspend fun getWeatherInsight(weatherSummary: String): String {
        return GeminiWorker.getWeatherInsight(weatherSummary)
    }

    suspend fun insertAlert(alert: NotificationAlert) {
        weatherDao.insertAlert(alert)
    }

    suspend fun clearAllAlerts() {
        weatherDao.clearAllAlerts()
    }
}
