package com.fangjet.care.presentation.pairing

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.care.R
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
    @param:StringRes val errorRes: Int? = null,
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
            _uiState.update { it.copy(code = digits, errorRes = null) }
        }

        fun onSubmit() {
            val state = _uiState.value
            if (!PairingCode.isValidFormat(state.code)) {
                _uiState.update { it.copy(errorRes = R.string.pairing_error_incomplete) }
                return
            }
            if (state.isSubmitting) return

            _uiState.update { it.copy(isSubmitting = true, errorRes = null) }
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
                                errorRes = R.string.pairing_error_wrong_code,
                            )
                        }

                    is RedeemResult.Expired ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorRes = R.string.pairing_error_expired,
                            )
                        }

                    is RedeemResult.NetworkError ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorRes = R.string.pairing_error_network,
                            )
                        }
                }
            }
        }
    }
