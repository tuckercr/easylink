package com.fangjet.launcher.data.permissions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

/**
 * Starts a phone call to [phoneNumber], degrading gracefully when CALL_PHONE
 * is not granted.
 *
 * With the permission it dials directly (ACTION_CALL). Without it, the dialer
 * opens pre-filled (ACTION_DIAL) so the call can still be placed with one tap —
 * far better than the attempt silently throwing a SecurityException, which for
 * Speed Dial or a fall alert would mean no call at all.
 */
fun placePhoneCall(
    context: Context,
    phoneNumber: String,
    permissions: PermissionChecker,
) {
    val action = if (permissions.hasCallPhone()) Intent.ACTION_CALL else Intent.ACTION_DIAL
    runCatching {
        context.startActivity(
            Intent(action, "tel:$phoneNumber".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { Log.e("PhoneCall", "Failed to start call to $phoneNumber", it) }
}
