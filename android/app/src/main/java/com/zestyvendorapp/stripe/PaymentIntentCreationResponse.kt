package com.zestyvendorapp.stripe

/**
 * Data model for create-payment-intent response from your backend
 */
data class PaymentIntentCreationResponse(
    val intent: String,
    val secret: String
)