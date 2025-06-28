package com.zestyvendorapp.stripe

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider

/**
 * Implements Stripe Terminal’s ConnectionTokenProvider by proxying to your ApiClient.
 */
class ConnectionTokenProvider : ConnectionTokenProvider {
  override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
    ApiClient.getConnectionTokenAsync(callback)
  }
}