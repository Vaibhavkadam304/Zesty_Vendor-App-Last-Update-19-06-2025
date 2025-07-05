package com.zestyvendorapp.stripe

import android.util.Log
import com.stripe.stripeterminal.TapToPay
import com.stripe.stripeterminal.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.exception.TerminalException

/**
 * Provides new connection tokens to the Stripe Terminal SDK.
 * Skips execution when in the TapToPay process.
 */
class ConnectionTokenProviderImpl : ConnectionTokenProvider {
  override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
    // Avoid initializing inside the isolated TapToPay process
    if (TapToPay.isInTapToPayProcess()) return

    Log.d("StripeTapToPay", ">> fetchConnectionToken()")
    ApiClient.getConnectionTokenAsync(object : ConnectionTokenCallback {
      override fun onSuccess(token: String) {
        Log.d("StripeTapToPay", "<< got token: $token")
        callback.onSuccess(token)
      }
      override fun onFailure(e: TerminalException) {
        Log.e("StripeTapToPay", "<< token failure", e)
        callback.onFailure(e)
      }
    })
  }
}