package com.fangjet.launcher.util

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import com.fangjet.launcher.R
import com.fangjet.launcher.presentation.tts.TtsViewModel

/**
 * Extension functions that wire TTS long-press into any [View] in one line.
 *
 * ## Design rationale
 *
 * The naive approach — adding `setOnLongClickListener` in every Fragment —
 * leads to duplication and inconsistency. An extension function centralises
 * the touch contract, the haptic feedback, and the label-resolution logic.
 *
 * ## Usage (in any Fragment)
 * ```kotlin
 * private val ttsViewModel: TtsViewModel by activityViewModels()
 *
 * override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *     binding.btnPhone.speakOnLongPress(ttsViewModel)
 *     binding.btnMessages.speakOnLongPress(ttsViewModel)
 *     binding.btnCamera.speakOnLongPress(ttsViewModel)
 *     // ...every button in one line each
 * }
 * ```
 *
 * That's all. The extension handles:
 *  - Reading the label from contentDescription or text
 *  - Haptic feedback on long-press (critical for vision-impaired UX)
 *  - Returning true to consume the event (prevents accidental triggering
 *    of other long-press actions)
 */
fun View.speakOnLongPress(
    ttsViewModel: TtsViewModel,
    overrideLabel: String? = null,
) {
    setOnLongClickListener { v ->
        val label = overrideLabel
            ?: v.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            ?: (v as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
            ?: v.resources.getResourceEntryName(v.id)

        // Haptic feedback — mandatory for vision-impaired users to confirm
        // the long-press was registered. performHapticFeedback is the
        // modern way (works without VIBRATE permission on API 26+).
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        ttsViewModel.speak(label)
        true // consume the event
    }

    // Make long-press discoverable — announce it in the accessibility tree
    isLongClickable = true
    tooltipText = context.getString(R.string.tts_tooltip)
}

/**
 * Convenience overload for when you want to speak a specific string
 * regardless of the view's own label — useful for app tiles in the
 * All Apps grid where the label comes from [AppInfo.label].
 */
fun View.speakOnLongPress(
    ttsViewModel: TtsViewModel,
    labelProvider: () -> String,
) {
    setOnLongClickListener { v ->
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        ttsViewModel.speak(labelProvider())
        true
    }
    isLongClickable = true
}
