package com.zestyvendorapp.stripe

import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Defines the HTTP endpoints your app will call on your terminal-server backend.
 */
interface BackendService {
  @FormUrlEncoded
  @POST("connection_token")
  fun getConnectionToken(): Call<ConnectionToken>

  @FormUrlEncoded
  @POST("create_payment_intent")
  fun createPaymentIntent(
    @FieldMap params: Map<String, String>
  ): Call<PaymentIntentCreationResponse>
}
