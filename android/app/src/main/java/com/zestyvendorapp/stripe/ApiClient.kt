package com.zestyvendorapp.stripe

import android.util.Log
import com.stripe.stripeterminal.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.exception.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback

/**
 * Singleton for making network calls to your backend.
 */
object ApiClient {
  private const val BASE_URL = "https://zestybakers.com/wp-json/zesty-terminal/v1/"

  private val client = OkHttpClient.Builder().build()
  private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  private val service = retrofit.create(BackendService::class.java)

  /**
   * Fetches the connection token asynchronously for the Terminal SDK.
   */
  fun getConnectionTokenAsync(callback: ConnectionTokenCallback) {
    Log.d("ApiClient", "Fetching connection token async...")

    service.getConnectionToken().enqueue(object : retrofit2.Callback<ConnectionToken> {
      override fun onResponse(
        call: retrofit2.Call<ConnectionToken>,
        response: retrofit2.Response<ConnectionToken>
      ) {
        if (response.isSuccessful && response.body()?.secret != null) {
          val secret = response.body()!!.secret
          Log.d("ApiClient", "Token fetch success: $secret")
          callback.onSuccess(secret)
        } else {
          Log.e("ApiClient", "Token fetch failed: No secret in response")
          callback.onFailure(ConnectionTokenException("No secret in response"))
        }
      }

      override fun onFailure(call: retrofit2.Call<ConnectionToken>, t: Throwable) {
        Log.e("ApiClient", "Token fetch network error: ${t.localizedMessage}")
        callback.onFailure(ConnectionTokenException("Network error: ${t.localizedMessage}"))
      }
    })
  }

  /**
   * Creates a PaymentIntent via your backend.
   */
  fun createPaymentIntent(
    amount: Long,
    currency: String,
    skipTipping: Boolean,
    callback: Callback<PaymentIntentCreationResponse>
  ) {
    val params = mutableMapOf<String, String>().apply {
      put("amount", amount.toString())
      put("currency", currency)
      put("locationId", "tml_GFZlfAmzFCGtcQ")
    }
    service.createPaymentIntent(params).enqueue(callback)
  }
}
