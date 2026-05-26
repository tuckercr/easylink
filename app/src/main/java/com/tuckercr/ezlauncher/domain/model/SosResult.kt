package com.tuckercr.ezlauncher.domain.model

/**
 * The outcome of a triggered SOS event.
 *
 * Returned by [TriggerSosUseCase] so the ViewModel can render
 * accurate feedback without knowing any Android API details.
 */
sealed class SosResult {

    /**
     * SOS fully dispatched.
     *
     * @param calledContact   The contact that was called (null if call was skipped)
     * @param smsRecipients   How many contacts received an SMS
     * @param locationShared  Whether GPS coordinates were included in the SMS
     */
    data class Success(
        val calledContact: EmergencyContact?,
        val smsRecipients: Int,
        val locationShared: Boolean,
    ) : SosResult()

    /** No emergency contacts are configured. */
    data object NoContactsConfigured : SosResult()

    /** Location permission was denied — SMS sent without coordinates. */
    data class PartialSuccess(
        val smsRecipients: Int,
        val reason: String,
    ) : SosResult()

    /** A hard failure (e.g. SMS permission denied, no SIM card). */
    data class Failure(
        val reason: String,
    ) : SosResult()
}
