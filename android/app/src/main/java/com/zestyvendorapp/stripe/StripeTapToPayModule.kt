package com.zestyvendorapp.stripe

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.Callback as TerminalCallback
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.CollectConfiguration
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
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
        Terminal.initTerminal(
          jsContext.applicationContext,
          LogLevel.VERBOSE,
          ConnectionTokenProvider(),
          object : com.stripe.stripeterminal.external.callable.TerminalListener {
            override fun onUnexpectedReaderDisconnect(reader: Reader) {}
            override fun onConnectionStatusChange(status: ConnectionStatus) {}
            override fun onPaymentStatusChange(status: PaymentStatus) {}
          }
        )
        isTerminalInitialized = true
      }
      promise.resolve(null)
    } catch (e: TerminalException) {
      promise.reject("INIT_ERROR", e.localizedMessage)
    }
  }

  @ReactMethod
  fun discoverReaders(locationId: String, promise: Promise) {
    val config = DiscoveryConfiguration(
      timeout = 10,
      discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
      isSimulated = true,
      location = locationId
    )

    Terminal.getInstance().discoverReaders(
      config,
      // 1) real‐time updates
      object : DiscoveryListener {
        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
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
      // 2) final completion callback
      object : TerminalCallback {
        override fun onSuccess() {
          promise.resolve(null)
        }
        override fun onFailure(e: TerminalException) {
          promise.reject("DISCOVER_ERROR", e.localizedMessage)
        }
      }
    )
  }

  @ReactMethod
  fun connectReader(readerId: String, locationId: String, promise: Promise) {
    val reader = discoveredReaders.find { it.id == readerId }
    if (reader == null) {
      promise.reject("NO_READER", "No reader found with id $readerId")
      return
    }
    val connConfig = ConnectionConfiguration.LocalMobileConnectionConfiguration(locationId)
    Terminal.getInstance().connectLocalMobileReader(
      reader,
      connConfig,
      object : ReaderCallback {
        override fun onSuccess(connectedReader: Reader) {
          promise.resolve(null)
        }
        override fun onFailure(e: TerminalException) {
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
            promise.reject("BACKEND_ERROR", "No client secret in response")
            return
          }
          Terminal.getInstance().retrievePaymentIntent(
            secret,
            object : PaymentIntentCallback {
              override fun onSuccess(pi: PaymentIntent) {
                val collectConfig = CollectConfiguration.Builder()
                  .skipTipping(skipTipping)
                  .build()
                Terminal.getInstance().collectPaymentMethod(
                  pi,
                  object : PaymentIntentCallback {
                    override fun onSuccess(pi: PaymentIntent) {
                      Terminal.getInstance().processPayment(
                        pi,
                        object : PaymentIntentCallback {
                          override fun onSuccess(pi: PaymentIntent) {
                            promise.resolve(null)
                          }
                          override fun onFailure(e: TerminalException) {
                            promise.reject("PROCESS_ERROR", e.localizedMessage)
                          }
                        }
                      )
                    }
                    override fun onFailure(e: TerminalException) {
                      promise.reject("COLLECT_ERROR", e.localizedMessage)
                    }
                  },
                  collectConfig
                )
              }
              override fun onFailure(e: TerminalException) {
                promise.reject("RETRIEVE_ERROR", e.localizedMessage)
              }
            }
          )
        }
        override fun onFailure(call: Call<PaymentIntentCreationResponse>, t: Throwable) {
          promise.reject("BACKEND_ERROR", t.localizedMessage)
        }
      }
    )
  }
}
