package com.fangjet.launcher.domain.model

import android.graphics.drawable.Drawable

/**
 * Pure domain model representing an installed application.
 *
 * This class lives in the domain layer and has zero Android framework
 * dependencies except [Drawable], which is unavoidable for icons.
 * It is deliberately a simple data class — no logic lives here.
 */
data class AppInfo(
    /** Unique package name (e.g. "com.google.android.dialer") */
    val packageName: String,
    /** User-visible label (e.g. "Phone") */
    val label: String,
    /** Launcher icon — nullable so the UI can show a placeholder */
    val icon: Drawable?,
) : Comparable<AppInfo> {
    /** Natural sort order: alphabetical by label, case-insensitive. */
    override fun compareTo(other: AppInfo): Int = label.compareTo(other.label, ignoreCase = true)
}
