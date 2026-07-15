package com.fangjet.launcher.domain.usecase

import com.fangjet.launcher.domain.model.SosResult
import com.fangjet.launcher.domain.repository.SosRepository
import javax.inject.Inject

/**
 * Use Case: trigger a full SOS event.
 *
 * This is the most important use case in the app — it's the one that,
 * if it fails silently, could have real consequences. The design
 * principles here reflect that:
 *
 *  1. It returns a rich [SosResult] — not a Boolean, not an exception.
 *     The ViewModel must always know *what* succeeded and *what* didn't.
 *
 *  2. It never throws. All error paths produce [SosResult.Failure] or
 *     [SosResult.PartialSuccess] so the UI can display appropriate feedback.
 *
 *  3. It delegates every platform concern to the repository — this use
 *     case is fully unit-testable with a mock SosRepository.
 */
class TriggerSosUseCase @Inject constructor(
    private val repository: SosRepository,
) {
    suspend operator fun invoke(): SosResult = repository.triggerSos()
}
