package com.zestyvendorapp.stripe

import android.util.Log
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider as StripeConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException

/**
 * Your implementation of Stripe’s ConnectionTokenProvider,
 * now with entry/exit logging so you see exactly what’s happening.
 */
class ConnectionTokenProvider : StripeConnectionTokenProvider {

  override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
    Log.d("StripeTapToPay", ">> fetchConnectionToken() invoked")

    // Wrap the callback so we can log success/failure at this boundary
    ApiClient.getConnectionTokenAsync(object : ConnectionTokenCallback {
      override fun onSuccess(token: String) {
        Log.d("StripeTapToPay", "<< fetchConnectionToken success, token=$token")
        callback.onSuccess(token)
      }

      override fun onFailure(exception: ConnectionTokenException) {
        Log.e("StripeTapToPay", "<< fetchConnectionToken failure", exception)
        callback.onFailure(exception)
      }
    })
  }
}