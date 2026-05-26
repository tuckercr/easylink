package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.repository.TtsRepository
import javax.inject.Inject

/**
 * Use Case: speak a UI label aloud.
 *
 * This sits between the ViewModel and [TtsRepository] and owns one
 * piece of business logic: sanitise the label before handing it to
 * the engine. Raw button labels may contain newlines, icon descriptions,
 * or emoji that sound terrible when read out — this is the right place
 * to strip them, not the Fragment.
 *
 * Usage in a ViewModel:
 * ```kotlin
 * fun onButtonLongPressed(label: String) = speakLabel(label)
 * ```
 */
class SpeakLabelUseCase @Inject constructor(
    private val repository: TtsRepository,
) {
    /**
     * Sanitise and speak [rawLabel].
     *
     * Sanitisation rules:
     *  - Collapse whitespace / newlines to a single space
     *  - Strip emoji and non-speech Unicode (surrogates, private-use area)
     *  - Trim leading/trailing whitespace
     *  - Guard against blank strings after sanitisation
     */
    operator fun invoke(rawLabel: String) {
        val cleaned = rawLabel
            .replace(Regex("[\\n\\r\\t]+"), " ") // newlines → space
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}]+"), "") // emoji / undefined chars
            .replace(Regex("\\s{2,}"), " ") // collapse multi-spaces
            .trim()

        if (cleaned.isNotBlank()) {
            repository.speak(cleaned)
        }
    }
}
