package com.zestyvendorapp.stripe

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException

/**
 * Implements Stripe Terminal’s ConnectionTokenProvider by proxying to your ApiClient.
 */
class ConnectionTokenProvider : ConnectionTokenProvider {
  override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
    try {
      val token = ApiClient.createConnectionToken()
      callback.onSuccess(token)
    } catch (e: ConnectionTokenException) {
      callback.onFailure(e)
    }
  }
}
