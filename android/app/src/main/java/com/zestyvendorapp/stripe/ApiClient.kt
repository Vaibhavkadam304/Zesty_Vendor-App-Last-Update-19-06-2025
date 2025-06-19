package com.zestyvendorapp.stripe

import com.stripe.stripeterminal.external.models.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import java.io.IOException

/**
 * Singleton for making network calls to your backend.
 */
object ApiClient {
  private const val BASE_URL = "http://192.168.232.21:3000/"

  private val client = OkHttpClient.Builder().build()
  private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  private val service = retrofit.create(BackendService::class.java)

  // Synchronously fetch a connection token for the Terminal SDK
  @Throws(ConnectionTokenException::class)
  fun createConnectionToken(): String {
    val response = service.getConnectionToken().execute()
    if (response.isSuccessful && response.body() != null) {
      return response.body()!!.secret
    }
    throw ConnectionTokenException("Creating connection token failed")
  }

  // Asynchronously create a PaymentIntent on your backend
  fun createPaymentIntent(
    amount: Long,
    currency: String,
    skipTipping: Boolean,
    callback: Callback<PaymentIntentCreationResponse>
  ) {
    val params = mutableMapOf<String, String>().apply {
      put("amount", amount.toString())
      put("currency", currency)
      if (skipTipping) {
        put("payment_method_options[card_present][skip_tipping]", "true")
      }
    }
    service.createPaymentIntent(params).enqueue(callback)
  }
}
