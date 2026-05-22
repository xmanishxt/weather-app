package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.NotificationAlert
import com.example.data.local.SavedLocation
import com.example.data.remote.GeocodingResult
import com.example.data.remote.WeatherResponse
import com.example.ui.components.MiniatureTrendChart
import com.example.ui.components.WeatherHelper
import com.example.ui.components.WeatherCodeDetails
import com.example.ui.theme.*
import com.example.ui.viewmodel.SearchState
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val notificationAlerts by viewModel.notificationAlerts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val severeEnabled by viewModel.severeAlertsEnabled.collectAsStateWithLifecycle()
    val rainEnabled by viewModel.rainAlertsEnabled.collectAsStateWithLifecycle()
    val dailyEnabled by viewModel.dailySummaryEnabled.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<SavedLocation?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Notifications/Telemetry

    // Handle scroll state for elegant single-screen transition
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBg),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "A E R O",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 6.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BlackBg
                )
            )
        },
        containerColor = BlackBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BlackBg)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar for geocoding input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchCity(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input"),
                    placeholder = {
                        Text(
                            text = "Search city to geocode...",
                            color = MutedText,
                            fontSize = 14.sp
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = PolarCyan
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = PolarCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.searchCity("")
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // Autocomplete Suggestions display in dynamic block
                AnimatedVisibility(
                    visible = searchState is SearchState.Searching || searchState is SearchState.Results || searchState is SearchState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Divider(color = GrayBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        when (val state = searchState) {
                            is SearchState.Searching -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PolarCyan,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            is SearchState.Results -> {
                                if (state.results.isEmpty()) {
                                    Text(
                                        text = "No cities matching query found.",
                                        color = MutedText,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    state.results.take(5).forEach { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.addSearchedLocation(result)
                                                    searchQuery = ""
                                                    viewModel.searchCity("")
                                                    focusManager.clearFocus()
                                                }
                                                .padding(vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocationOn,
                                                    contentDescription = "Pin",
                                                    tint = IceBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "${result.name}${if (result.state != null) ", " + result.state else ""}",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = result.country ?: "",
                                                color = MutedText,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Divider(color = GrayBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                                    }
                                }
                            }
                            is SearchState.Error -> {
                                Text(
                                    text = state.message,
                                    color = PyroRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // 2. Saved Locations Selection Chips Row with press animations
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MONITORED CLIMATES",
                    color = MutedText,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(savedLocations, key = { it.id }) { loc ->
                        val isSelected = loc.isCurrent
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) DarkSurfaceCard else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PolarCyan else GrayBorder,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .combinedClickable(
                                    onClick = { 
                                        viewModel.makeLocationActive(loc)
                                        focusManager.clearFocus()
                                    },
                                    onLongClick = {
                                        // Reykjavik cannot be deleted to avoid clearing seeding
                                        if (loc.name != "Reykjavik") {
                                            showDeleteDialog = loc
                                        }
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("location_chip_${loc.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.LocationOn else Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    tint = if (isSelected) PolarCyan else MutedText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = loc.name,
                                    color = if (isSelected) Color.White else WarmText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (loc.name != "Reykjavik" && isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hold to delete",
                                        tint = PyroRed.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clickable { showDeleteDialog = loc }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Dialog for delete
            showDeleteDialog?.let { loc ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "Unmonitor Climate?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove ${loc.name} from your dashboard alerts?",
                            color = WarmText,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteLocation(loc.id)
                                showDeleteDialog = null
                            }
                        ) {
                            Text("Unmonitor", color = PyroRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                )
            }

            // 3. Tab Selectors (Dashboard / Notification Telemetry System)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = BlackBg,
                contentColor = PolarCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = PolarCyan,
                        height = 1.dp
                    )
                },
                divider = { Divider(color = GrayBorder, thickness = 0.5.dp) }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { 
                        activeTab = 0 
                        focusManager.clearFocus()
                    },
                    text = {
                        Text(
                            "DASHBOARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = MutedText
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { 
                        activeTab = 1 
                        focusManager.clearFocus()
                    },
                    text = {
                        Text(
                            "TELEMETRY & ALERTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = MutedText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Tab Views
            if (activeTab == 0) {
                // DASHBOARD VIEW
                when (val state = uiState) {
                    is WeatherUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PolarCyan)
                        }
                    }
                    is WeatherUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = "Error",
                                tint = PyroRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = state.message,
                                color = WarmText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.refreshActiveLocation() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                            ) {
                                Text("Retry connection", color = PolarCyan)
                            }
                        }
                    }
                    is WeatherUiState.Success -> {
                        val response = state.response
                        val current = response.current
                        val hourly = response.hourly
                        val daily = response.daily

                        if (current != null) {
                            val weatherCode = current.weatherCode
                            val details: WeatherCodeDetails = WeatherHelper.getWeatherDetails(weatherCode)

                            // Weather Display Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = state.activeLocation.name.uppercase(),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 2.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${state.activeLocation.state}, ${state.activeLocation.country}".uppercase(),
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            color = MutedText
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.refreshActiveLocation() },
                                        modifier = Modifier.border(0.5.dp, GrayBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        if (isRefreshing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = PolarCyan,
                                                strokeWidth = 1.5.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Refresh",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Main temperature giant font
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = details.icon,
                                        contentDescription = "Weather Code Design Icon",
                                        tint = details.color,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text(
                                        text = "${current.temperature.toInt()}°",
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        fontFamily = FontFamily.SansSerif,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = details.description.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = details.color,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Divider(color = GrayBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Weather Attributes Grid (Wind, humidity, felt temp, precip)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.Air,
                                            contentDescription = null,
                                            tint = IceBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Wind", fontSize = 9.sp, color = MutedText)
                                        Text("${current.windSpeed.toInt()} kmh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.WaterDrop,
                                            contentDescription = null,
                                            tint = PolarCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Humidity", fontSize = 9.sp, color = MutedText)
                                        Text("${current.humidity.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.Thermostat,
                                            contentDescription = null,
                                            tint = SolarGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Feels like", fontSize = 9.sp, color = MutedText)
                                        Text("${current.apparentTemperature.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.Umbrella,
                                            contentDescription = null,
                                            tint = AuroraGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Precip", fontSize = 9.sp, color = MutedText)
                                        Text("${current.precipitation} mm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            // 4. AI Weather Recommendation Insight Bubble
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(DarkSurface, BlackBg)
                                        )
                                    )
                                    .border(1.dp, PolarCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = PolarCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AERO INTELLIGENCE DESIGN INSIGHT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = PolarCyan
                                    )
                                }
                                Text(
                                    text = "\"${state.insight}\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Light,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                )
                            }

                            // 5. Hourly custom chart visualizations
                            if (hourly != null && hourly.temperatureList.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "HOURLY TEMPERATURE TREND",
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = MutedText,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Extract next 12 hours from response
                                    val temperatures12h = hourly.temperatureList.take(12)
                                    
                                    // Parse times to visual HH:00 labels
                                    val times12h = hourly.time.take(12).map { isoTime ->
                                        try {
                                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                                            val formatter = SimpleDateFormat("HH:mm", Locale.US)
                                            val date = parser.parse(isoTime)
                                            if (date != null) formatter.format(date) else ""
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }

                                    // Custom drawn Canvas Area sparkline chart
                                    MiniatureTrendChart(
                                        temperatures = temperatures12h,
                                        times = times12h,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    )
                                }
                            }

                            // 6. Detailed Horizontal Hourly Scroll List
                            if (hourly != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "CHRONOLOGICAL FORECAST",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Take next 24 hours
                                        val itemsCount = hourly.time.size.coerceAtMost(24)
                                        items((0 until itemsCount).toList()) { index ->
                                            val isoTime = hourly.time[index]
                                            val hourTemp = hourly.temperatureList[index]
                                            val hourCode = hourly.weatherCodeList[index]
                                            val hourPrecip = hourly.precipitationProbability[index]

                                            val hourDetails = WeatherHelper.getWeatherDetails(hourCode)

                                            val timeLabel = try {
                                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                                                val formatter = SimpleDateFormat("HH:mm", Locale.US)
                                                parser.parse(isoTime)?.let { formatter.format(it) } ?: ""
                                            } catch (e: Exception) {
                                                ""
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(DarkSurface)
                                                    .border(0.5.dp, GrayBorder, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(timeLabel, fontSize = 10.sp, color = MutedText)
                                                Icon(
                                                    imageVector = hourDetails.icon,
                                                    contentDescription = null,
                                                    tint = hourDetails.color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text("${hourTemp.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Umbrella,
                                                        contentDescription = null,
                                                        tint = PolarCyan,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                    Text("$hourPrecip%", fontSize = 7.sp, color = PolarCyan, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 7. Robust 7-Day Forecast System
                            if (daily != null) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "7-DAY EXTENDED METEORO-GRAPH",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        daily.time.indices.forEach { index ->
                                            val isoDay = daily.time[index]
                                            val dayCode = daily.weatherCodeList[index]
                                            val dayMax = daily.tempMax[index]
                                            val dayMin = daily.tempMin[index]
                                            val dayPrecip = daily.precipitationSum[index]

                                            val dayDetails = WeatherHelper.getWeatherDetails(dayCode)

                                            val dayLabel = try {
                                                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                val formatter = SimpleDateFormat("EEEE, MMM d", Locale.US)
                                                parser.parse(isoDay)?.let { formatter.format(it) } ?: ""
                                            } catch (e: Exception) {
                                                ""
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(DarkSurface)
                                                    .border(0.5.dp, GrayBorder, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1.5f)) {
                                                    Text(dayLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(dayDetails.description, fontSize = 10.sp, color = MutedText)
                                                }

                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = dayDetails.icon,
                                                        contentDescription = null,
                                                        tint = dayDetails.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    if (dayPrecip > 0.0) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("${dayPrecip.toInt()}mm", fontSize = 9.sp, color = PolarCyan, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${dayMax.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("${dayMin.toInt()}°", fontSize = 11.sp, color = MutedText)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 1. TELEMETRY, SYSTEM SETTINGS & NOTIFICATION CONFIG
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Alert settings card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "REAL-TIME CHANNELS CONFIG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolarCyan,
                            letterSpacing = 1.sp
                        )

                        // 1. Severe Alerts Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Emergency Severe Warnings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Post notifications instantly when rain levels/storms pose outdoor risks.", fontSize = 10.sp, color = MutedText)
                            }
                            Switch(
                                checked = severeEnabled,
                                onCheckedChange = { viewModel.severeAlertsEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PolarCyan,
                                    checkedTrackColor = PolarCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = MutedText,
                                    uncheckedTrackColor = DarkBgSurface()
                                ),
                                modifier = Modifier.testTag("severe_warnings_toggle")
                            )
                        }

                        Divider(color = GrayBorder, thickness = 0.5.dp)

                        // 2. Rain warning Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Precipitation & Rain Warnings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Receive smart updates when local coordinates detect rain drops.", fontSize = 10.sp, color = MutedText)
                            }
                            Switch(
                                checked = rainEnabled,
                                onCheckedChange = { viewModel.rainAlertsEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PolarCyan,
                                    checkedTrackColor = PolarCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = MutedText,
                                    uncheckedTrackColor = DarkBgSurface()
                                ),
                                modifier = Modifier.testTag("rain_warnings_toggle")
                            )
                        }

                        Divider(color = GrayBorder, thickness = 0.5.dp)

                        // 3. Daily Summary toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Morning Summaries", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Get a complete local forecast block in your tray first thing in the morning.", fontSize = 10.sp, color = MutedText)
                            }
                            Switch(
                                checked = dailyEnabled,
                                onCheckedChange = { viewModel.dailySummaryEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PolarCyan,
                                    checkedTrackColor = PolarCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = MutedText,
                                    uncheckedTrackColor = DarkBgSurface()
                                )
                            )
                        }
                    }

                    // Test notification controller card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SYSTEM TELEMETRY TESTING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Exercise the real-time background channels immediately by posting an instant mock climate update notification into your device's native notification drawer.",
                            fontSize = 10.sp,
                            color = MutedText
                        )

                        Button(
                            onClick = { viewModel.triggerManualTestNotification() },
                            colors = ButtonDefaults.buttonColors(containerColor = PolarCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("trigger_notification_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = BlackBg
                                )
                                Text(
                                    text = "TRIGGER NATIVE SYSTEM NOTIFICATION",
                                    color = BlackBg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Log of Alerts (backed by database)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HISTORICAL ALERT TELEMETRY LOG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            if (notificationAlerts.isNotEmpty()) {
                                Text(
                                    text = "CLEAR ALL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PyroRed,
                                    modifier = Modifier.clickable { viewModel.clearAlertLogs() }
                                )
                            }
                        }

                        if (notificationAlerts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = AuroraGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Telemetry quiet. No warnings logged.",
                                        color = MutedText,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                notificationAlerts.forEach { alert ->
                                    val alertTimeStr = SimpleDateFormat("HH:mm:ss dd.MM", Locale.US).format(Date(alert.timestamp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BlackBg)
                                            .border(0.5.dp, GrayBorder, RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (alert.type) {
                                                "SEVERE" -> Icons.Rounded.Warning
                                                "RAIN" -> Icons.Rounded.WaterDrop
                                                else -> Icons.Rounded.CheckCircle
                                            },
                                            contentDescription = null,
                                            tint = when (alert.type) {
                                                "SEVERE" -> PyroRed
                                                "RAIN" -> PolarCyan
                                                else -> AuroraGreen
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(alert.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(alertTimeStr, fontSize = 8.sp, color = MutedText)
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(alert.message, fontSize = 10.sp, color = WarmText, lineHeight = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DarkBgSurface(): Color = Color(0xFF1E1E1E)
