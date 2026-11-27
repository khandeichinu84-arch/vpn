# Project specific ProGuard rules

# Keep core VPN classes safe
-keep class com.example.** { *; }
-keep class com.example.ultimateworldvpn.** { *; }
-dontwarn com.example.ultimateworldvpn.**

# Obfuscate to prevent reverse engineering
-repackageclasses 'com.secure.world.core'
-allowaccessmodification

# Keep Android components referenced in Manifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Compose & Kotlin reflection
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-dontwarn okio.**
-dontwarn retrofit2.**
