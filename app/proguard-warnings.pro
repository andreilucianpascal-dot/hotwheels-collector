# ========== WARNING SUPPRESSIONS ==========
# This file contains ProGuard/R8 rules to suppress warnings during the build process
# These rules don't affect app functionality, they just clean up build output

# Suppress warnings about missing classes (common in large projects)
-dontwarn javax.annotation.**
-dontwarn java.lang.management.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.tools.**
-dontwarn sun.misc.Unsafe

# Suppress Firebase warnings
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.auto.value.**

# Suppress ML Kit warnings  
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

# Suppress Retrofit warnings
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Suppress Kotlin warnings
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**

# Suppress Compose warnings
-dontwarn androidx.compose.**
-dontwarn androidx.navigation.**
-dontwarn androidx.activity.**
-dontwarn androidx.lifecycle.**

# Suppress Room warnings
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# Suppress Hilt warnings
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# Suppress CameraX warnings
-dontwarn androidx.camera.**

# Suppress Glide warnings  
-dontwarn com.bumptech.glide.**

# Suppress Accompanist warnings
-dontwarn com.google.accompanist.**

# Suppress Coil warnings
-dontwarn coil.**

# Suppress WorkManager warnings
-dontwarn androidx.work.**

# Suppress Paging warnings
-dontwarn androidx.paging.**

# Suppress DataStore warnings
-dontwarn androidx.datastore.**

# Suppress Security warnings
-dontwarn androidx.security.**
-dontwarn androidx.biometric.**

# Suppress Timber warnings
-dontwarn timber.log.**

# Suppress test framework warnings
-dontwarn org.junit.**
-dontwarn junit.**
-dontwarn org.mockito.**
-dontwarn org.robolectric.**