package com.fangjet.launcher.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCheckerTest {

    private val context: Context = mockk()
    private val checker = PermissionChecker(context)

    /** Grant exactly [granted]; everything else is denied. */
    private fun grantOnly(vararg granted: String) {
        every { context.checkSelfPermission(any()) } returns PackageManager.PERMISSION_DENIED
        granted.forEach {
            every { context.checkSelfPermission(it) } returns PackageManager.PERMISSION_GRANTED
        }
    }

    @Test
    fun `isGranted reflects the underlying context check`() {
        grantOnly(Manifest.permission.SEND_SMS)
        assertTrue(checker.isGranted(Manifest.permission.SEND_SMS))
        assertFalse(checker.isGranted(Manifest.permission.CALL_PHONE))
    }

    @Test
    fun `semantic helpers map to their permissions`() {
        grantOnly(Manifest.permission.CALL_PHONE, Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(checker.hasSendSms())
        assertTrue(checker.hasCallPhone())
        assertTrue(checker.hasFineLocation())
    }

    @Test
    fun `missingSosRequired lists both SMS and call when neither granted`() {
        grantOnly() // nothing granted
        assertEquals(
            listOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE),
            checker.missingSosRequired(),
        )
        assertFalse(checker.isSosReady())
    }

    @Test
    fun `missingSosRequired is empty and SOS ready when SMS and call granted`() {
        grantOnly(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
        assertTrue(checker.missingSosRequired().isEmpty())
        assertTrue(checker.isSosReady())
    }

    @Test
    fun `location alone does not make SOS ready`() {
        grantOnly(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(checker.isSosReady())
        assertEquals(
            listOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE),
            checker.missingSosRequired(),
        )
    }

    @Test
    fun `missingSosAll includes location when only SMS and call are granted`() {
        grantOnly(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            checker.missingSosAll(),
        )
    }

    @Test
    fun `missingSosAll is empty when everything granted`() {
        grantOnly(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        assertTrue(checker.missingSosAll().isEmpty())
    }
}
