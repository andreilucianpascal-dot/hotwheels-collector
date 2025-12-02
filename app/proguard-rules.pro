# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========== WARNING SUPPRESSIONS ==========
# Include warning suppression rules from separate file
-include proguard-warnings.pro

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** {
    *;
}

# Keep ML Kit classes (but exclude problematic native libraries)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep CameraX classes
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** {
    *;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Glide classes
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Keep Retrofit and OkHttp classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepattributes InnerClasses
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Timber
-keep class timber.log.** { *; }

# 16 KB Alignment Compatibility Rules
# Exclude problematic native libraries that cause 16 KB alignment issues
-keep class !com.google.mlkit.vision.** { *; }
-keep class !com.google.mlkit.barcode.** { *; }
-keep class !com.google.mlkit.text.** { *; }

# Force modern native library handling
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions,InnerClasses

# Handle native library conflicts (proper syntax)
-dontwarn com.google.mlkit.vision.internal.**
-dontwarn com.google.android.gms.internal.**

# Keep essential ML Kit functionality while excluding problematic parts
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.mlkit.** { *; }

# Exclude problematic ML Kit native dependencies
-keep class !com.google.mlkit.vision.internal.** { *; }
-keep class !com.google.mlkit.vision.internal.vkp.** { *; }

# Fix missing classes detected by R8
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Keep Google Crypto Tink classes
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep OkHttp2 classes (legacy)
-keep class com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

# Keep Joda Time classes  
-keep class org.joda.time.** { *; }
-dontwarn org.joda.time.**

# Keep gRPC classes
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**