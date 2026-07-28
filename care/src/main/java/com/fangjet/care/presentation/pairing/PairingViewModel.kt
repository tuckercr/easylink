package com.fangjet.care.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.care.data.pairing.CarePairingRepository
import com.fangjet.care.data.pairing.RedeemResult
import com.fangjet.shared.PairingCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Non-null once pairing succeeded; the elder's display name. */
    val pairedTo: String? = null,
) {
    val canSubmit: Boolean get() = PairingCode.isValidFormat(code) && !isSubmitting
}

@HiltViewModel
class PairingViewModel
    @Inject
    constructor(
        private val repository: CarePairingRepository,
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(PairingUiState())
        val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

        /**
         * Accepts digits only and stops at [PairingCode.LENGTH].
         *
         * Filtering here rather than in the composable means a paste of
         * "123-456" still works, which is how codes actually arrive when a
         * sibling texts one over.
         */
        fun onCodeChanged(input: String) {
            val digits = input.filter { it.isDigit() }.take(PairingCode.LENGTH)
            _uiState.update { it.copy(code = digits, error = null) }
        }

        fun onSubmit() {
            val state = _uiState.value
            if (!PairingCode.isValidFormat(state.code)) {
                _uiState.update { it.copy(error = "Enter all 6 digits.") }
                return
            }
            if (state.isSubmitting) return

            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                when (val result = repository.redeem(state.code)) {
                    is RedeemResult.Success ->
                        _uiState.update {
                            it.copy(isSubmitting = false, pairedTo = result.elderDisplayName)
                        }

                    is RedeemResult.InvalidCode ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = "That code isn't right. Double-check and try again.",
                            )
                        }

                    is RedeemResult.Expired ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = "That code has expired or was already used. Ask for a new one.",
                            )
                        }

                    is RedeemResult.NetworkError ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = "Couldn't connect. Check your internet and try again.",
                            )
                        }
                }
            }
        }
    }
