# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep general Kotlin & reflection attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# Preserve AppConfig and DEFAULT_CONFIG
-keep class com.example.AppConfig { *; }
-keep class com.example.DEFAULT_CONFIG { *; }

# Preserve Data Models, DB Entities, and API DTOs
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Preserve ViewModels and Activity
-keep class com.example.ui.ChatViewModel { *; }
-keep class com.example.MainActivity { *; }

# Retrofit & OkHttp ProGuard Rules
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Moshi ProGuard Rules
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Room Database ProGuard Rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# Keep Coroutines
-dontwarn kotlinx.coroutines.**
