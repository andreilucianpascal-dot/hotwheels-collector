plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.hotwheelscollectors"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.hotwheelscollectors"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
        
        // Force 16 KB alignment compatibility
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            ndkVersion = "25.2.9519653"
        }
    }

    signingConfigs {
        create("release") {
            // Add your release signing config here when ready for production
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-warnings.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        // Suppress common Kotlin warnings
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.camera.core.ExperimentalGetImage",
            "-opt-in=androidx.paging.ExperimentalPagingApi",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",

            // Suppress specific warning categories
            "-Xjvm-default=all",
            "-Xskip-prerelease-check",
            "-Xallow-result-return-type",
            "-Xsuppress-version-warnings",

            // Suppress unused parameter/variable warnings
            "-Xno-param-assertions",
            "-Xno-call-assertions",

            // Suppress deprecation and type warnings
            "-Xsuppress-deprecated-jvm-target-warning"
        )

        // Additional warning suppressions
        allWarningsAsErrors = false
        suppressWarnings = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
        prefab = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = false
            
            // Handle conflicting native libraries
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/libjsc.so"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        quiet = true // Reduces lint output verbosity
        ignoreWarnings = true // Ignore lint warnings (keeps errors)

        // Reference the suppressions file
        lintConfig = file("src/main/res/xml/suppressions.xml")

        // Disable specific warning categories
        disable += setOf(
            "InvalidPackage",
            "OldTargetApi",
            "IconMissingDensityFolder",
            "IconDensities",
            "IconLocation",
            "IconDuplicatesConfig",
            "GoogleAppIndexingWarning",
            "UnusedResources",
            "UnusedIds",
            "ContentDescription",
            "HardcodedText",
            "RelativeOverlap",
            "RtlHardcoded",
            "RtlSymmetry",
            "SetTextI18n",
            "Overdraw",
            "UseAppTint",
            "GradleOverrides",
            "NewerVersionAvailable",
            "GradleDependency",
            "KtxExtensionAvailable",
            "ObsoleteLintCustomCheck",
            "LintError",
            "StaticFieldLeak",
            "WrongThread"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // ---------- Core Android ----------
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- Google Drive SDK ---
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.api-client:google-api-client-android:1.34.0")
    implementation("com.google.api-client:google-api-client-gson:1.34.0")
    implementation("com.google.http-client:google-http-client-android:1.42.3")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // --- Dropbox SDK ---
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.6")

    // ---------- Compose ----------
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.foundation:foundation")

    // ---------- Navigation ----------
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ---------- Hilt Dependency Injection ----------
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // ---------- Room Database ----------
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ---------- Paging ----------
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // ---------- WorkManager ----------
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.work:work-multiprocess:2.9.0")
    implementation("androidx.work:work-gcm:2.9.0")

    // ---------- CameraX (16 KB Compatible) ----------
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")

    // ---------- Modern ML Kit Alternatives (16 KB Compatible) ----------
    // Barcode scanning - using Google Play Services
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
    // Text recognition - using Google Play Services
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    // Alternative barcode scanning with ZXing
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Alternative OCR with Google Vision API
    implementation("com.google.android.gms:play-services-vision:20.1.3")

    // ---------- Alternative ML Kit Implementations (16 KB Compatible) ----------
    // ML Kit Vision Kit - Alternative implementation (16 KB compatible)
    implementation("com.google.mlkit:vision-common:17.3.0")
    
    // ML Kit Barcode Scanning - Core only (16 KB compatible)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // ML Kit Text Recognition - Core only (16 KB compatible)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // ---------- AndroidX Security Crypto ----------
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // ---------- Glide ----------
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:annotations:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    // ---------- Google Sign-In ----------
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-drive:17.0.0")

    // ---------- ML Kit (Google Play Services) ----------
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // ---------- Firebase (Essential Services Only) ----------
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")

    // ---------- DataStore ----------
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-core:1.0.0")

    // ---------- Coil ----------
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")

    // ---------- Kotlinx Serialization ----------
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // ---------- Gson ----------
    implementation("com.google.code.gson:gson:2.10.1")

    // ---------- Coroutines ----------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ---------- Retrofit & Networking ----------
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // ---------- Image Processing ----------
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    
    // ---------- TensorFlow Lite ----------
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")  // Optional - pentru GPU acceleration
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // ---------- OpenCV (Post-processing pentru TFLite masks) ----------
    // OpenCV Android SDK - modul oficial importat
    // NOTĂ: Modulul opencv trebuie să fie copiat în root-ul proiectului (vezi OPENCV_SETUP_PAS_CU_PAS.md)
    implementation(project(":opencv"))

    // ---------- Permissions ----------
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")

    // ---------- System UI Controller ----------
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.35.0-alpha")

    // ---------- Swipe Refresh ----------
    implementation("com.google.accompanist:accompanist-swiperefresh:0.35.0-alpha")

    // ---------- Pager ----------
    implementation("com.google.accompanist:accompanist-pager:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.35.0-alpha")

    // ---------- Placeholder ----------
    implementation("com.google.accompanist:accompanist-placeholder:0.35.0-alpha")

    // ---------- Navigation Animation ----------
    implementation("com.google.accompanist:accompanist-navigation-animation:0.35.0-alpha")

    // ---------- Timber for Professional Logging ----------
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ---------- Professional Performance Monitoring ----------
    implementation("androidx.metrics:metrics-performance:1.0.0-alpha04")
    implementation("androidx.tracing:tracing:1.2.0")

    // ---------- Professional App Features ----------
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.window:window:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ---------- Testing Dependencies ----------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("com.jakewharton.timber:timber:5.0.1")

    // ---------- Android Testing ----------
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("com.jakewharton.timber:timber:5.0.1")

    // ---------- Hilt Testing Dependencies ----------
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
    androidTestImplementation("androidx.work:work-testing:2.9.0")

    // ---------- LeakCanary for Memory Leak Detection (Debug Only) ----------
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    // Note: No-op version has repository issues, but not needed since LeakCanary 
    // automatically does nothing in release builds when not included
}