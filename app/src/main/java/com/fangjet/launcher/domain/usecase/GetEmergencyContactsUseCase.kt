package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.repository.SosRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: observe the live list of configured emergency contacts. */
class GetEmergencyContactsUseCase @Inject constructor(
    private val repository: SosRepository,
) {
    operator fun invoke(): Flow<List<EmergencyContact>> = repository.getEmergencyContacts()
}
