package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.TodayReminder
import com.fangjet.launcher.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: get today's chronological medication schedule. */
class GetTodayRemindersUseCase @Inject constructor(
    private val repository: MedicationRepository,
) {
    operator fun invoke(): Flow<List<TodayReminder>> = repository.getTodayReminders()
}
