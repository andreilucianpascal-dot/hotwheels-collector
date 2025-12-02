// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
        classpath("com.android.tools.build:gradle:8.6.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("buildAll") {
    dependsOn(":app:assembleDebug")
    dependsOn(":app:assembleRelease")
}

tasks.register("testAll") {
    dependsOn(":app:testDebugUnitTest")
    dependsOn(":app:testReleaseUnitTest")
}

tasks.register("lintAll") {
    dependsOn(":app:lintDebug")
    dependsOn(":app:lintRelease")
}

tasks.register("cleanAll") {
    dependsOn("clean")
    dependsOn(":app:clean")
}