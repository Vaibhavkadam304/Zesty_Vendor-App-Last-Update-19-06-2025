# Prevent R8 from stripping java.beans annotations (needed by Jackson)
-keepattributes *Annotation*
-keepclassmembers class * {
    @java.beans.** *;
}
-dontwarn java.beans.**

# ───────────────────────────────
# ✅ Stripe SDK
# ───────────────────────────────
-keep class com.stripe.** { *; }

# ───────────────────────────────
# ✅ Retrofit models (like ConnectionToken, PaymentIntentCreationResponse)
# ───────────────────────────────
-keep class com.zestyvendorapp.stripe.** { *; }

# ───────────────────────────────
# ✅ Prevent Gson from stripping JSON fields like "secret"
# ───────────────────────────────
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ───────────────────────────────────────────────────
# ✅ Preserve Stripe AIDL binder interfaces & stubs
# ───────────────────────────────────────────────────
-keep class com.stripe.core.aidlrpc.** { *; }
-keep class com.stripe.terminal.service.** { *; }
