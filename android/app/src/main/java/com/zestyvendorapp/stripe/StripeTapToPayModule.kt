package com.zestyvendorapp.stripe

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.Callback as TerminalCallback
import com.stripe.stripeterminal.external.models.*
import com.stripe.stripeterminal.log.LogLevel
import retrofit2.Call
import retrofit2.Callback as RetrofitCallback
import retrofit2.Response

class StripeTapToPayModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  companion object {
    private var isTerminalInitialized = false
  }

  private val jsContext = reactContext
  private var discoveredReaders: List<Reader> = emptyList()

  override fun getName(): String = "StripeTapToPay"

  @ReactMethod
  fun initialize(promise: Promise) {
    try {
      if (!isTerminalInitialized) {
        Log.d("StripeTapToPay", "Initializing Terminal SDK")
        Terminal.initTerminal(
          jsContext.applicationContext,
          LogLevel.VERBOSE,
          ConnectionTokenProvider(),
          object : com.stripe.stripeterminal.external.callable.TerminalListener {
            override fun onUnexpectedReaderDisconnect(reader: Reader) {
              Log.w("StripeTapToPay", "Unexpected reader disconnect: ${reader.serialNumber}")
            }
            override fun onConnectionStatusChange(status: ConnectionStatus) {
              Log.d("StripeTapToPay", "Connection status changed: $status")
            }
            override fun onPaymentStatusChange(status: PaymentStatus) {
              Log.d("StripeTapToPay", "Payment status changed: $status")
            }
          }
        )
        isTerminalInitialized = true
      } else {
        Log.d("StripeTapToPay", "Terminal already initialized. Skipping re-init.")
      }
      promise.resolve(null)
    } catch (e: TerminalException) {
      Log.e("StripeTapToPay", "Initialization error: ${e.errorMessage}")
      promise.reject("INIT_ERROR", e.localizedMessage)
    }
  }

  @ReactMethod
  fun discoverReaders(locationId: String, promise: Promise) {
    Log.d("StripeTapToPay", "Starting discoverReaders with locationId: $locationId")

    val config = DiscoveryConfiguration(
      timeout = 10,
      discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
      isSimulated = false,
      location = locationId
    )

    Terminal.getInstance().discoverReaders(
      config,
      object : DiscoveryListener {
        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
          Log.d("StripeTapToPay", "Discovered ${readers.size} readers")
          readers.forEach { Log.d("StripeTapToPay", "Reader: ${it.id}, label: ${it.deviceType}") }
          discoveredReaders = readers
          val arr = Arguments.createArray()
          readers.forEach { r ->
            Arguments.createMap().apply {
              putString("id", r.id)
              putString("label", r.deviceType.toString())
            }.also { arr.pushMap(it) }
          }
          jsContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("readersDiscovered", arr)
        }
      },
      object : TerminalCallback {
        override fun onSuccess() {
          Log.d("StripeTapToPay", "Reader discovery completed successfully")
          promise.resolve(null)
        }
        override fun onFailure(e: TerminalException) {
          Log.e("StripeTapToPay", "Discovery failed: ${e.errorMessage}")
          promise.reject("DISCOVER_ERROR", e.localizedMessage)
        }
      }
    )
  }

  @ReactMethod
  fun connectReader(readerId: String, locationId: String, promise: Promise) {
    Log.d("StripeTapToPay", "Attempting to connect to readerId: $readerId for locationId: $locationId")
    val reader = discoveredReaders.find { it.id == readerId }
    if (reader == null) {
      Log.e("StripeTapToPay", "No reader found with id: $readerId")
      promise.reject("NO_READER", "No reader found with id $readerId")
      return
    }

    val connConfig = ConnectionConfiguration.LocalMobileConnectionConfiguration(locationId)
    Terminal.getInstance().connectLocalMobileReader(
      reader,
      connConfig,
      object : ReaderCallback {
        override fun onSuccess(connectedReader: Reader) {
          Log.d("StripeTapToPay", "Successfully connected to reader: ${connectedReader.serialNumber}")
          promise.resolve(null)
        }

        override fun onFailure(e: TerminalException) {
          Log.e("StripeTapToPay", "Failed to connect to reader: ${e.errorMessage}")
          promise.reject("CONNECT_ERROR", e.localizedMessage)
        }
      }
    )
  }

  @ReactMethod
  fun collectAndProcessPayment(
    amount: Double,
    currency: String,
    skipTipping: Boolean,
    promise: Promise
  ) {
    Log.d("StripeTapToPay", "Starting collectAndProcessPayment for amount: $amount $currency")
    ApiClient.createPaymentIntent(
      (amount * 100).toLong(),
      currency,
      skipTipping,
      object : RetrofitCallback<PaymentIntentCreationResponse> {
        override fun onResponse(
          call: Call<PaymentIntentCreationResponse>,
          response: Response<PaymentIntentCreationResponse>
        ) {
          val secret = response.body()?.secret
          if (secret == null) {
            Log.e("StripeTapToPay", "Backend returned null client secret")
            promise.reject("BACKEND_ERROR", "No client secret in response")
            return
          }
          Log.d("StripeTapToPay", "Retrieved client secret successfully")
          Terminal.getInstance().retrievePaymentIntent(
            secret,
            object : PaymentIntentCallback {
              override fun onSuccess(pi: PaymentIntent) {
                Log.d("StripeTapToPay", "PaymentIntent retrieved successfully")
                val collectConfig = CollectConfiguration.Builder()
                  .skipTipping(skipTipping)
                  .build()
                Terminal.getInstance().collectPaymentMethod(
                  pi,
                  object : PaymentIntentCallback {
                    override fun onSuccess(pi: PaymentIntent) {
                      Log.d("StripeTapToPay", "PaymentMethod collected successfully")
                      Terminal.getInstance().processPayment(
                        pi,
                        object : PaymentIntentCallback {
                          override fun onSuccess(pi: PaymentIntent) {
                            Log.d("StripeTapToPay", "Payment processed successfully")
                            promise.resolve(null)
                          }
                          override fun onFailure(e: TerminalException) {
                            Log.e("StripeTapToPay", "Processing payment failed: ${e.errorMessage}")
                            promise.reject("PROCESS_ERROR", e.localizedMessage)
                          }
                        }
                      )
                    }
                    override fun onFailure(e: TerminalException) {
                      Log.e("StripeTapToPay", "Collect payment failed: ${e.errorMessage}")
                      promise.reject("COLLECT_ERROR", e.localizedMessage)
                    }
                  },
                  collectConfig
                )
              }
              override fun onFailure(e: TerminalException) {
                Log.e("StripeTapToPay", "Retrieve PaymentIntent failed: ${e.errorMessage}")
                promise.reject("RETRIEVE_ERROR", e.localizedMessage)
              }
            }
          )
        }
        override fun onFailure(call: Call<PaymentIntentCreationResponse>, t: Throwable) {
          Log.e("StripeTapToPay", "Backend call failed: ${t.localizedMessage}")
          promise.reject("BACKEND_ERROR", t.localizedMessage)
        }
      }
    )
  }
}