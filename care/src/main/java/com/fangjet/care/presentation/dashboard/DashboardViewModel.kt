package com.fangjet.care.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.care.data.config.CareConfigRepository
import com.fangjet.care.data.pairing.CarePairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val elderName: String = "",
    val contactCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        pairingRepository: CarePairingRepository,
        configRepository: CareConfigRepository,
    ) : ViewModel() {

        val uiState: StateFlow<DashboardUiState> =
            pairingRepository.linkId
                .filterNotNull()
                .flatMapLatest { linkId ->
                    combine(
                        configRepository.observeElderName(linkId),
                        configRepository.observeEmergencyContacts(linkId),
                    ) { name, contacts ->
                        DashboardUiState(elderName = name, contactCount = contacts.size)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DashboardUiState(),
                )
    }
