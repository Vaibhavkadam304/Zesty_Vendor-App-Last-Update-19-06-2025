package com.zestyvendorapp.stripe

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import android.util.Log

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

  // ✅ NEW: Asynchronous connection token fetch
  fun getConnectionTokenAsync(callback: ConnectionTokenCallback) {
    Log.d("ApiClient", "Fetching connection token async...")

    service.getConnectionToken().enqueue(object : retrofit2.Callback<ConnectionToken> {
      override fun onResponse(
        call: retrofit2.Call<ConnectionToken>,
        response: retrofit2.Response<ConnectionToken>
      ) {
        if (response.isSuccessful && response.body()?.secret != null) {
          Log.d("ApiClient", "Token fetch success: ${response.body()!!.secret}")
          callback.onSuccess(response.body()!!.secret)
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

  // ✅ Keep this for payment creation — it's fine
  fun createPaymentIntent(
    amount: Long,
    currency: String,
    skipTipping: Boolean,
    callback: Callback<PaymentIntentCreationResponse>
  ) {
    val params = mutableMapOf<String, String>().apply {
      put("amount", amount.toString())
      put("locationId", "tml_GFZlfAmzFCGtcQ")
    }
    service.createPaymentIntent(params).enqueue(callback)
  }

  // ❌ (Optional) Remove the old sync version — not used anymore
  /*
  @Throws(ConnectionTokenException::class)
  fun createConnectionToken(): String {
    ...
  }
  */
}
