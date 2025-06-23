package com.zestyvendorapp.stripe

import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface BackendService {

  // FIXED: No @FormUrlEncoded since there are no fields
  @POST("connection_token")
  fun getConnectionToken(): Call<ConnectionToken>

  // Leave this as-is since you're sending form data for createPaymentIntent
  @FormUrlEncoded
  @POST("create_payment_intent")
  fun createPaymentIntent(
    @FieldMap params: Map<String, String>
  ): Call<PaymentIntentCreationResponse>
}