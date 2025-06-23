package com.zestyvendorapp.stripe

/**
 * Data model for create-payment-intent response from your backend
 */
data class PaymentIntentCreationResponse(
    val paymentIntent: PaymentIntentData
)

data class PaymentIntentData(
    val id: String,
    val client_secret: String
)