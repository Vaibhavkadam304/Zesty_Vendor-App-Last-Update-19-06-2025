package com.zestyvendorapp.stripe

/**
 * A one-field data class used to handle the connection token response from our backend
 */
data class ConnectionToken(val secret: String)