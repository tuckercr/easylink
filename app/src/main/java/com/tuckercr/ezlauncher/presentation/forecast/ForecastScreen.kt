package com.tuckercr.ezlauncher.presentation.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class ForecastUiState {
    data object Loading : ForecastUiState()
    data class Success(
        val city: String?,
        val days: List<ForecastDay>,
    ) : ForecastUiState()

    data class Error(val message: String) : ForecastUiState()
}

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherService: WeatherService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.value = ForecastUiState.Loading
        viewModelScope.launch {
            try {
                val (city, days) = weatherService.fetchForecast()
                _uiState.value = if (days.isEmpty()) {
                    ForecastUiState.Error("Location unavailable — make sure location permission is granted")
                } else {
                    ForecastUiState.Success(city = city, days = days)
                }
            } catch (e: Exception) {
                _uiState.value = ForecastUiState.Error(e.message ?: "Failed to load forecast")
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
            Text(
                text = when (val s = state) {
                    is ForecastUiState.Success -> s.city ?: "7-Day Forecast"
                    else -> "7-Day Forecast"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (val s = state) {
            is ForecastUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ForecastUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            is ForecastUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(s.days) { day ->
                        ForecastRow(day = day)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            thickness = 1.dp,
                        )
                    }
                }
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
        // Emoji
        Text(
            text = day.emoji,
            fontSize = 34.sp,
        )

        Spacer(Modifier.width(14.dp))

        // Day label + description
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

        // Rain chance
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

        // High / Low
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
