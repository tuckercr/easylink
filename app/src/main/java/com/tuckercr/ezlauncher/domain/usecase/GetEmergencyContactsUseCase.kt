package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.EmergencyContact
import com.tuckercr.ezlauncher.domain.repository.SosRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: observe the live list of configured emergency contacts. */
class GetEmergencyContactsUseCase @Inject constructor(
    private val repository: SosRepository,
) {
    operator fun invoke(): Flow<List<EmergencyContact>> = repository.getEmergencyContacts()
}
