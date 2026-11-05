# VidSave ProGuard Rules

# Keep Moshi JSON generated adapters & models
-keep class com.vidsave.app.data.model.** { *; }
-keep class com.vidsave.app.data.remote.** { *; }

# Keep Room entities and DAOs
-keep class com.vidsave.app.data.local.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers enum * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# FileProvider
-keep class androidx.core.content.FileProvider { *; }
