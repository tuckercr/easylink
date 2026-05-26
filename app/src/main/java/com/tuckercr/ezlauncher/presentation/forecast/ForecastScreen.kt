package com.tuckercr.ezlauncher.presentation.forecast

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tuckercr.ezlauncher.R
import com.tuckercr.ezlauncher.data.weather.WeatherService
import com.tuckercr.ezlauncher.domain.model.ForecastDay
import com.tuckercr.ezlauncher.domain.model.ForecastResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────────────────────

sealed class ForecastUiState {
    data object Loading : ForecastUiState()

    data class Success(
        val city: String?,
        val days: List<ForecastDay>,
        val usingCachedLocation: Boolean = false,
    ) : ForecastUiState()

    /** Location permission not granted. */
    data object PermissionNeeded : ForecastUiState()

    /**
     * Location services are off and there is no saved location to fall back on.
     * The UI should offer a button to open system location settings.
     */
    data object LocationDisabled : ForecastUiState()

    /** Network error or no location fix (and no saved location). */
    data class Error(
        val message: String,
    ) : ForecastUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherService: WeatherService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = ForecastUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = weatherService.fetchForecast()) {
                is ForecastResult.Success -> ForecastUiState.Success(
                    city = result.city,
                    days = result.days,
                    usingCachedLocation = result.usingCachedLocation,
                )

                is ForecastResult.PermissionNeeded -> ForecastUiState.PermissionNeeded
                is ForecastResult.LocationDisabled -> ForecastUiState.LocationDisabled
                is ForecastResult.Unavailable -> ForecastUiState.Error(
                    "Could not get a location fix. Make sure location services are on and try again.",
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val RainBlue = Color(0xFF64B5F6)

@Composable
fun ForecastScreen(
    onBack: () -> Unit,
    viewModel: ForecastViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column {
                Text(
                    text = when (val s = state) {
                        is ForecastUiState.Success -> s.city ?: "7-Day Forecast"
                        else -> "7-Day Forecast"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // Subtle "saved location" note when using cached coords
                if (state is ForecastUiState.Success &&
                    (state as ForecastUiState.Success).usingCachedLocation
                ) {
                    Text(
                        text = "📍 Saved location",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (val s = state) {
            is ForecastUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ForecastUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(s.days) { day ->
                        ForecastRow(day = day)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            thickness = 1.dp,
                        )
                    }
                }
            }

            is ForecastUiState.LocationDisabled -> {
                ForecastInfoBox(
                    emoji = "📍",
                    title = "Location is off",
                    body = "Turn on location services so the forecast can use your current position.",
                    actionLabel = "Open Location Settings",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                )
            }

            is ForecastUiState.PermissionNeeded -> {
                ForecastInfoBox(
                    emoji = "🌤️",
                    title = "Location permission needed",
                    body = "Grant location permission to EZ Launcher so the forecast knows where you are.",
                    actionLabel = "Open App Settings",
                    onAction = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data =
                                    android.net.Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                )
            }

            is ForecastUiState.Error -> {
                ForecastInfoBox(
                    emoji = "🌡️",
                    title = "Forecast unavailable",
                    body = s.message,
                    actionLabel = "Retry",
                    onAction = { viewModel.retry() },
                )
            }
        }
    }
}

// ── Shared info/error box ─────────────────────────────────────────────────────

@Composable
private fun ForecastInfoBox(
    emoji: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, fontSize = 52.sp)
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) {
                Text(actionLabel, fontSize = 16.sp)
            }
        }
    }
}

// ── Forecast row ──────────────────────────────────────────────────────────────

@Composable
private fun ForecastRow(day: ForecastDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = day.emoji, fontSize = 34.sp)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.dayLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = day.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }

        if (day.precipitationChance > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 14.dp),
            ) {
                Text("💧", fontSize = 13.sp)
                Text(
                    text = "${day.precipitationChance}%",
                    fontSize = 14.sp,
                    color = RainBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Spacer(Modifier.width(52.dp))
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = day.displayMax,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = day.displayMin,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}
