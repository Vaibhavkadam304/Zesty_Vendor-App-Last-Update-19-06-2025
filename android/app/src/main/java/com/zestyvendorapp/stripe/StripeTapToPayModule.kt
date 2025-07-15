
// StripeTapToPayModule.kt
package com.zestyvendorapp.stripe

import android.app.Application
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

import com.stripe.stripeterminal.log.LogLevel
import com.stripe.stripeterminal.tap2pay.TapToPay
import com.stripe.stripeterminal.tap2pay.TapToPayListener
import com.stripe.stripeterminal.tap2pay.TapToPayConnectionConfiguration
import com.stripe.stripeterminal.tap2pay.TapToPayDiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.tap2pay.TapToPayReaderListener
import com.stripe.stripeterminal.model.PaymentIntent
import com.stripe.stripeterminal.exception.TerminalException

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
            if (!isTerminalInitialized && !TapToPay.isInTapToPayProcess()) {
                // Application lifecycle delegate must be called once
                com.stripe.stripeterminal.TerminalApplicationDelegate.onCreate(
                    jsContext.applicationContext as Application
                )

                // Init Tap to Pay SDK
                TapToPay.initTapToPay(
                    jsContext.applicationContext,
                    ConnectionTokenProviderImpl(),
                    LogLevel.VERBOSE
                )

                // Register listener for Tap to Pay events
                TapToPay.getInstance().registerTapToPayListener(object : TapToPayListener {
                    override fun onTapToPayFlowStarted() {
                        jsContext.getJSModule(RCTDeviceEventEmitter::class.java)
                            .emit("tapToPayStarted", null)
                    }

                    override fun onTapToPayFlowFinished(paymentIntent: PaymentIntent) {
                        val result = Arguments.createMap().apply {
                            putString("status", paymentIntent.status)
                        }
                        jsContext.getJSModule(RCTDeviceEventEmitter::class.java)
                            .emit("tapToPayFinished", result)
                    }

                    override fun onTapToPayFlowCancelled() {
                        jsContext.getJSModule(RCTDeviceEventEmitter::class.java)
                            .emit("tapToPayCancelled", null)
                    }

                    override fun onError(e: Exception) {
                        val errorMap = Arguments.createMap().apply {
                            putString("message", e.localizedMessage)
                        }
                        jsContext.getJSModule(RCTDeviceEventEmitter::class.java)
                            .emit("tapToPayError", errorMap)
                    }
                })

                isTerminalInitialized = true
            }
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.localizedMessage, e)
        }
    }

    @ReactMethod
    fun startTapToPay(clientSecret: String, promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "Activity not available")
            return
        }
        TapToPay.getInstance().startTapToPayFlow(
            activity,
            clientSecret
        )
        promise.resolve(null)
    }
}
