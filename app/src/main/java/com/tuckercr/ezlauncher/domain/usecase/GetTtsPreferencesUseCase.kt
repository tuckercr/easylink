package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.TtsPreferences
import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use Case: observe live TTS preferences from DataStore. */
class GetTtsPreferencesUseCase @Inject constructor(
    private val repository: TtsRepository,
) {
    operator fun invoke(): Flow<TtsPreferences> = repository.getPreferences()
}
