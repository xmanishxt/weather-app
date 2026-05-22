package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
    val state: String = "",
    val isCurrent: Boolean = false
)

@Entity(tableName = "notification_alerts")
data class NotificationAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // "ALERT", "UPDATE", "NOTICE"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    @Query("UPDATE saved_locations SET isCurrent = 0")
    suspend fun clearCurrentLocations()

    @Transaction
    suspend fun makeActiveLocation(location: SavedLocation) {
        clearCurrentLocations()
        insertLocation(location.copy(isCurrent = true))
    }

    @Query("SELECT * FROM saved_locations WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentLocation(): SavedLocation?

    // Alerts
    @Query("SELECT * FROM notification_alerts ORDER BY timestamp DESC LIMIT 50")
    fun getAllAlerts(): Flow<List<NotificationAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: NotificationAlert)

    @Query("UPDATE notification_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAlertRead(id: Int)

    @Query("DELETE FROM notification_alerts")
    suspend fun clearAllAlerts()
}

@Database(entities = [SavedLocation::class, NotificationAlert::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
