package com.fangjet.launcher.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central, testable gate for runtime-permission checks.
 *
 * Wrapping [Context.checkSelfPermission] in one injectable place means
 * features can ask questions in domain terms ("is SOS ready?") instead of
 * repeating `ContextCompat.checkSelfPermission(...) == GRANTED` everywhere,
 * and the logic can be unit-tested with a mocked [Context].
 */
@Singleton
class PermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun isGranted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun hasSendSms(): Boolean = isGranted(Manifest.permission.SEND_SMS)

    fun hasCallPhone(): Boolean = isGranted(Manifest.permission.CALL_PHONE)

    fun hasFineLocation(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Of [permissions], the ones not currently granted (order preserved). */
    fun missing(permissions: List<String>): List<String> = permissions.filterNot { isGranted(it) }

    /**
     * Permissions SOS cannot function without — texting and calling contacts.
     * Location is a bonus (shared in the SMS) but not required.
     */
    fun missingSosRequired(): List<String> = missing(SOS_REQUIRED)

    /** All SOS-relevant permissions that are missing, including location. */
    fun missingSosAll(): List<String> = missing(SOS_ALL)

    /** True when SOS can both text and call its contacts. */
    fun isSosReady(): Boolean = missingSosRequired().isEmpty()

    companion object {
        val SOS_REQUIRED: List<String> = listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
        )

        val SOS_ALL: List<String> = SOS_REQUIRED + Manifest.permission.ACCESS_FINE_LOCATION
    }
}
