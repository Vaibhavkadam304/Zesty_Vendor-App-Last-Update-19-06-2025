package com.zestyvendorapp.stripe

import com.stripe.stripeterminal.external.models.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import java.io.IOException
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

  // Synchronously fetch a connection token for the Terminal SDK
  @Throws(ConnectionTokenException::class)
  fun createConnectionToken(): String {
      Log.d("ApiClient", "Attempting to fetch connection token...")

      val response = service.getConnectionToken().execute()

      Log.d("ApiClient", "HTTP response code: ${response.code()}")
      Log.d("ApiClient", "HTTP response success: ${response.isSuccessful}")

      val responseBody = response.body()
      if (response.isSuccessful && responseBody != null) {
          Log.d("ApiClient", "Received token: ${responseBody.secret}")
          return responseBody.secret
      }

      Log.e("ApiClient", "Connection token fetch failed: ${response.errorBody()?.string()}")
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
      put("locationId", "tml_GFZlfAmzFCGtcQ")  
  }
  service.createPaymentIntent(params).enqueue(callback)
  }
}
