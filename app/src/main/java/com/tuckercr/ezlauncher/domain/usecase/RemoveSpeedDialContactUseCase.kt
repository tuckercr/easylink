package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.SpeedDialContact
import com.tuckercr.ezlauncher.domain.repository.SpeedDialRepository
import javax.inject.Inject

/**
 * Removes a pinned contact from the Speed Dial grid.
 *
 * The remaining contacts retain their relative order — display order
 * indices are compacted by the repository implementation on delete.
 */
class RemoveSpeedDialContactUseCase @Inject constructor(
    private val repository: SpeedDialRepository,
) {
    suspend operator fun invoke(contact: SpeedDialContact) =
        repository.removeContact(contact)
}
