package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AuroraGreen
import com.example.ui.theme.IceBlue
import com.example.ui.theme.PolarCyan
import com.example.ui.theme.PyroRed
import com.example.ui.theme.SolarGold

data class WeatherCodeDetails(
    val description: String,
    val icon: ImageVector,
    val color: Color
)

object WeatherHelper {

    fun getWeatherDetails(code: Int): WeatherCodeDetails {
        return when (code) {
            0 -> WeatherCodeDetails("Clear sky", Icons.Rounded.WbSunny, SolarGold)
            1 -> WeatherCodeDetails("Mainly clear", Icons.Rounded.WbSunny, SolarGold)
            2 -> WeatherCodeDetails("Partly cloudy", Icons.Rounded.Cloud, IceBlue)
            3 -> WeatherCodeDetails("Overcast", Icons.Rounded.Cloud, MutedCloudColor())
            45, 48 -> WeatherCodeDetails("Foggy conditions", Icons.Rounded.Grain, IceBlue)
            51, 53, 55 -> WeatherCodeDetails("Light drizzle", Icons.Rounded.WaterDrop, PolarCyan)
            56, 57 -> WeatherCodeDetails("Freezing drizzle", Icons.Rounded.AcUnit, IceBlue)
            61 -> WeatherCodeDetails("Slight rain", Icons.Rounded.WaterDrop, PolarCyan)
            63 -> WeatherCodeDetails("Moderate rain", Icons.Rounded.WaterDrop, PolarCyan)
            65 -> WeatherCodeDetails("Heavy rain", Icons.Rounded.Thunderstorm, PyroRed)
            66, 67 -> WeatherCodeDetails("Freezing rain", Icons.Rounded.AcUnit, IceBlue)
            71, 73, 75 -> WeatherCodeDetails("Snowfall", Icons.Rounded.AcUnit, Color.White)
            77 -> WeatherCodeDetails("Snow grains", Icons.Rounded.AcUnit, Color.White)
            80, 81, 82 -> WeatherCodeDetails("Rain showers", Icons.Rounded.WaterDrop, PolarCyan)
            85, 86 -> WeatherCodeDetails("Snow showers", Icons.Rounded.AcUnit, Color.White)
            95 -> WeatherCodeDetails("Thunderstorm", Icons.Rounded.FlashOn, SolarGold)
            96, 99 -> WeatherCodeDetails("Thunderstorm with hail", Icons.Rounded.FlashOn, PyroRed)
            else -> WeatherCodeDetails("Unknown weather", Icons.Rounded.Air, PolarCyan)
        }
    }

    private fun MutedCloudColor(): Color = Color(0xFFAAAAAA)
}
