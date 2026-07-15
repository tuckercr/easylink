package com.fangjet.launcher.data.repository

import android.content.Context
import com.fangjet.launcher.R
import com.fangjet.launcher.domain.model.EmergencyContact
import com.fangjet.launcher.domain.model.SosResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SosResultResolverTest {

    private val context: Context = mockk()

    @Before
    fun setup() {
        every { context.getString(R.string.sos_error_no_permissions) } returns "Turn on permissions"
        every { context.getString(R.string.sos_error_generic) } returns "Please call for help"
        every { context.getString(R.string.sos_error_partial_no_sms) } returns "SMS permission off"
    }

    private val alice =
        EmergencyContact(id = 1, name = "Alice", phoneNumber = "5551234", isPrimary = true)

    @Test
    fun `failure with permission hint when SMS blocked and no call placed`() {
        val result = resolveSosResult(
            context = context,
            smsPermitted = false,
            smsSent = 0,
            callPlaced = false,
            locationShared = false,
            calledContact = alice,
        )
        assertTrue(result is SosResult.Failure)
        assertTrue((result as SosResult.Failure).reason.contains("permission", ignoreCase = true))
    }

    @Test
    fun `failure with generic hint when permitted but nothing got through`() {
        val result = resolveSosResult(
            context = context,
            smsPermitted = true,
            smsSent = 0,
            callPlaced = false,
            locationShared = false,
            calledContact = alice,
        )
        assertTrue(result is SosResult.Failure)
        assertTrue((result as SosResult.Failure).reason.contains("call for help", ignoreCase = true))
    }

    @Test
    fun `partial success when SMS blocked but call was placed`() {
        val result = resolveSosResult(
            context = context,
            smsPermitted = false,
            smsSent = 0,
            callPlaced = true,
            locationShared = false,
            calledContact = alice,
        )
        assertTrue(result is SosResult.PartialSuccess)
        result as SosResult.PartialSuccess
        assertEquals(0, result.smsRecipients)
        assertTrue(result.reason.contains("SMS", ignoreCase = true))
    }

    @Test
    fun `full success reports called contact, count, and location`() {
        val result = resolveSosResult(
            context = context,
            smsPermitted = true,
            smsSent = 3,
            callPlaced = true,
            locationShared = true,
            calledContact = alice,
        )
        assertTrue(result is SosResult.Success)
        result as SosResult.Success
        assertEquals(alice, result.calledContact)
        assertEquals(3, result.smsRecipients)
        assertTrue(result.locationShared)
    }

    @Test
    fun `success with no called contact when call failed but texts sent`() {
        val result = resolveSosResult(
            context = context,
            smsPermitted = true,
            smsSent = 2,
            callPlaced = false,
            locationShared = false,
            calledContact = alice,
        )
        assertTrue(result is SosResult.Success)
        assertNull((result as SosResult.Success).calledContact)
        assertEquals(2, result.smsRecipients)
    }
}
