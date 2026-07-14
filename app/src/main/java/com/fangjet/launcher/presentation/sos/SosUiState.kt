package com.fangjet.launcher.presentation.sos

import com.fangjet.launcher.domain.model.SosResult

/**
 * All possible states of the SOS countdown screen.
 *
 * The sealed hierarchy makes the impossible states impossible:
 * you can't accidentally render a countdown of -1, or show a
 * result before the SOS has fired.
 */
sealed class SosUiState {

    /**
     * Countdown is ticking. The Fragment renders the number large and red,
     * and shows a prominent Cancel button.
     *
     * @param secondsRemaining  Counts down from COUNTDOWN_SECONDS to 1.
     */
    data class Counting(
        val secondsRemaining: Int,
    ) : SosUiState()

    /** SOS is in progress — location fetch + SMS + call underway. */
    data object Dispatching : SosUiState()

    /** SOS completed. Show result and navigate away after a brief pause. */
    data class Done(
        val result: SosResult,
    ) : SosUiState()

    /** User pressed Cancel — SOS was not sent. */
    data object Cancelled : SosUiState()

    companion object {
        const val COUNTDOWN_SECONDS = 5
    }
}
