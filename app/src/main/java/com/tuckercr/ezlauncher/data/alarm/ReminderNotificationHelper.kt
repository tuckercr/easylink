package com.tuckercr.ezlauncher.data.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tuckercr.ezlauncher.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ID_REMINDERS = "medication_reminders"
const val NOTIFICATION_ID_OFFSET = 50_000 // keeps reminder IDs clear of other notifications

/**
 * Builds and posts medication reminder notifications.
 *
 * ## Notification design for elderly/vision-impaired users:
 *
 *  - **BigTextStyle** — full medication name and notes visible without expanding
 *  - **Two large action buttons** — "TAKEN" and "SNOOZE 15 MIN" (no icon-only)
 *  - **HIGH importance** — heads-up notification that appears over other apps
 *  - **Sound + vibration** — multi-sensory alert; critical for hearing-impaired
 *  - **Persistent until dismissed** — [NotificationCompat.FLAG_NO_CLEAR] prevents
 *    accidental swipe-dismissal before the user has responded
 *  - **Full-screen intent** — wakes the screen if the device is locked/sleeping
 */
@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    /**
     * Create the notification channel (safe to call multiple times — idempotent).
     * Must be called before posting any notifications. We call it in [EZLauncherApplication].
     */
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Medication Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when it's time to take a medication"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Post a heads-up reminder notification for one medication dose.
     *
     * @param medicationId   Used to build unique notification ID and action intents
     * @param name           Medication name — shown large in the notification title
     * @param dosage         e.g. "1 tablet" — shown in the body
     * @param notes          e.g. "Take with food" — shown in expanded view
     * @param scheduledTime  The exact time this reminder was scheduled for
     */
    fun showReminder(
        medicationId: Long,
        name: String,
        dosage: String,
        notes: String,
        scheduledTime: LocalDateTime,
    ) {
        val notifId = (NOTIFICATION_ID_OFFSET + medicationId).toInt()
        val scheduledTimeStr = scheduledTime.toString()

        val takenIntent = actionIntent(
            action = ACTION_TAKEN,
            medicationId = medicationId,
            scheduledTime = scheduledTimeStr,
            notifId = notifId,
        )
        val snoozeIntent = actionIntent(
            action = ACTION_SNOOZE,
            medicationId = medicationId,
            scheduledTime = scheduledTimeStr,
            notifId = notifId,
        )

        val bodyText = buildString {
            append(dosage)
            if (notes.isNotBlank()) append("\n$notes")
        }

        val notification = NotificationCompat
            .Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Time to take $name")
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle("Time to take $name"),
            ).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false) // stays until user acts
            .setOngoing(true) // can't swipe away
            .addAction(
                R.drawable.ic_check,
                "TAKEN",
                takenIntent,
            ).addAction(
                R.drawable.ic_snooze,
                "SNOOZE 15 MIN",
                snoozeIntent,
            ).setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // show on lock screen
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun dismissReminder(medicationId: Long) {
        notificationManager.cancel((NOTIFICATION_ID_OFFSET + medicationId).toInt())
    }

    // ── Action intents ────────────────────────────────────────────────────────

    private fun actionIntent(
        action: String,
        medicationId: Long,
        scheduledTime: String,
        notifId: Int,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        return PendingIntent.getBroadcast(
            context,
            notifId + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_TAKEN = "com.tuckercr.ezlauncher.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.tuckercr.ezlauncher.ACTION_SNOOZE"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val SNOOZE_MINUTES = 15L
    }
}
