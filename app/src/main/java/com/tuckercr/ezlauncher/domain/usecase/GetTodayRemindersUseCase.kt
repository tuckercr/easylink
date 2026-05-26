package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.TodayReminder
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: get today's chronological medication schedule. */
class GetTodayRemindersUseCase @Inject constructor(
    private val repository: MedicationRepository,
) {
    operator fun invoke(): Flow<List<TodayReminder>> = repository.getTodayReminders()
}
