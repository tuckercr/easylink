package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.SpeedDialContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a reactive list of the user's pinned Speed Dial contacts,
 * sorted by [SpeedDialContact.displayOrder].
 *
 * The Flow emits a new list whenever the underlying data changes,
 * so the UI stays in sync without any manual refresh.
 */
class GetSpeedDialContactsUseCase @Inject constructor(
    private val repository: SpeedDialRepository,
) {
    operator fun invoke(): Flow<List<SpeedDialContact>> = repository.getSpeedDialContacts()
}
