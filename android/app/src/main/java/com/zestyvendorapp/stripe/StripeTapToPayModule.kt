package com.zestyvendorapp.stripe

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.stripe.stripeterminal.TapToPay
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.ApplicationDelegate as TerminalApplicationDelegate
import com.stripe.stripeterminal.callable.ReaderCallback
import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.exception.TerminalException
import com.stripe.stripeterminal.listener.TerminalListener
import com.stripe.stripeterminal.model.Reader
import com.stripe.stripeterminal.model.PaymentIntent
import com.stripe.stripeterminal.callable.CollectConfiguration
import com.stripe.stripeterminal.taptopay.TapToPayDiscoveryConfiguration
import com.stripe.stripeterminal.taptopay.TapToPayConnectionConfiguration
import com.stripe.stripeterminal.taptopay.TapToPayReaderListener

class StripeTapToPayModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  companion object {
    private var isTerminalInitialized = false
  }

  private val jsContext = reactContext
  private var discoveredReaders: List<Reader> = emptyList()

  override fun getName(): String = "StripeTapToPay"

  @ReactMethod
  fun initialize(promise: Promise) {
    try {
      // Skip SDK init inside the TapToPay process
      if (!isTerminalInitialized && !TapToPay.isInTapToPayProcess()) {
        // Application lifecycle delegate must be called once
        TerminalApplicationDelegate.onCreate(jsContext.applicationContext as Application)

        Terminal.initTerminal(
          jsContext.applicationContext,
          com.stripe.stripeterminal.LogLevel.VERBOSE,
          ConnectionTokenProviderImpl(),
          object : TerminalListener {
            override fun onConnectionStatusChange(status: com.stripe.stripeterminal.listener.ConnectionStatus) {
              Log.d("StripeTapToPay", "Connection status: $status")
            }
            override fun onUnexpectedReaderDisconnect(reader: Reader) {
              Log.w("StripeTapToPay", "Unexpected disconnect: ${'$'}{reader.serialNumber}")
            }
            override fun onPaymentStatusChange(status: com.stripe.stripeterminal.listener.PaymentStatus) {
              Log.d("StripeTapToPay", "Payment status: $status")
            }
          }
        )
        isTerminalInitialized = true
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("INIT_ERROR", e.localizedMessage, e)
    }
  }

  @ReactMethod
  fun discoverReaders(promise: Promise) {
    val config = TapToPayDiscoveryConfiguration(
      isSimulated = BuildConfig.DEBUG
    )

    Terminal.getInstance().discoverReaders(
      config,
      object : com.stripe.stripeterminal.discovery.DiscoveryListener {
        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
          discoveredReaders = readers
          val arr = Arguments.createArray().apply {
            readers.forEach { r ->
              val map = Arguments.createMap()
              map.putString("id", r.id)
              map.putString("label", r.deviceType.toString())
              pushMap(map)
            }
          }
          jsContext.getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("readersDiscovered", arr)
          promise.resolve(null)
        }
        override fun onFailure(e: TerminalException) {
          promise.reject("DISCOVER_ERROR", e.localizedMessage, e)
        }
      },
      object : com.stripe.stripeterminal.callable.Callback {
        override fun onSuccess() {}
        override fun onFailure(e: TerminalException) {}
      }
    )
  }

  @ReactMethod
  fun connectReader(promise: Promise) {
    if (discoveredReaders.isEmpty()) {
      promise.reject("NO_READER", "No readers discovered")
      return
    }
    val config = TapToPayConnectionConfiguration(
      locationId = "tml_GFZlfAmzFCGtcQ",
      autoReconnectOnUnexpectedDisconnect = true,
      readerListener = object : TapToPayReaderListener {
        override fun onDisconnect(reason: com.stripe.stripeterminal.taptopay.DisconnectReason) {
          Log.w("StripeTapToPay", "Reader disconnected: $reason")
        }
        override fun onReaderReconnectStarted(reader: Reader, cancelReconnect: com.stripe.stripeterminal.callable.Cancelable, reason: com.stripe.stripeterminal.taptopay.DisconnectReason) {}
        override fun onReaderReconnectSucceeded(reader: Reader) {}
        override fun onReaderReconnectFailed(reader: Reader) {}
      }
    )

    Terminal.getInstance().connectReader(
      discoveredReaders.first(),
      config,
      object : ReaderCallback {
        override fun onSuccess(reader: Reader) {
          promise.resolve(null)
        }
        override fun onFailure(e: TerminalException) {
          promise.reject("CONNECT_ERROR", e.localizedMessage, e)
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
      (amount * 100).toLong(), currency, skipTipping,
      object : retrofit2.Callback<PaymentIntentCreationResponse> {
        override fun onResponse(
          call: retrofit2.Call<PaymentIntentCreationResponse>,
          response: retrofit2.Response<PaymentIntentCreationResponse>
        ) {
          val secret = response.body()?.paymentIntent?.client_secret
          if (secret.isNullOrEmpty()) {
            promise.reject("BACKEND_ERROR", "No client secret")
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
                  pi, collectConfig,
                  object : PaymentIntentCallback {
                    override fun onSuccess(collectedPi: PaymentIntent) {
                      Terminal.getInstance().processPayment(
                        collectedPi,
                        object : PaymentIntentCallback {
                          override fun onSuccess(p: PaymentIntent) {
                            promise.resolve(null)
                          }
                          override fun onFailure(e: TerminalException) {
                            promise.reject("PROCESS_ERROR", e.localizedMessage, e)
                          }
                        }
                      )
                    }
                    override fun onFailure(e: TerminalException) {
                      promise.reject("COLLECT_ERROR", e.localizedMessage, e)
                    }
                  }
                )
              }
              override fun onFailure(e: TerminalException) {
                promise.reject("RETRIEVE_ERROR", e.localizedMessage, e)
              }
            }
          )
        }
        override fun onFailure(call: retrofit2.Call<PaymentIntentCreationResponse>, t: Throwable) {
          promise.reject("BACKEND_ERROR", t.localizedMessage, t)
        }
      }
    )
  }
}