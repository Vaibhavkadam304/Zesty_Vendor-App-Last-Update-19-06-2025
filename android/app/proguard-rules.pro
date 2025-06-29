# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
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
