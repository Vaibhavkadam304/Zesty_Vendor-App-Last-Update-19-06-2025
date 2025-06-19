package com.zestyvendorapp.stripe

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class StripeTapToPayPackage : ReactPackage {
  override fun createNativeModules(ctx: ReactApplicationContext): List<NativeModule> {
    return listOf(StripeTapToPayModule(ctx))
  }
  override fun createViewManagers(ctx: ReactApplicationContext): List<ViewManager<*, *>> {
    return emptyList()
  }
}
