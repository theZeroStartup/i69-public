buildscript {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath("com.android.tools:r8:8.2.42")
        classpath(Build.androidBuildTools)
        classpath(Build.kotlinPlugin)
        classpath(Build.googleGmsPlugin)
        classpath(Build.firebaseCrashlyticsPlugin)
        classpath(Build.navigationSafeArgs)
        classpath(Build.hiltAndroidPlugin)
        classpath(Build.oneSignalPlugin)
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
//        classpath("com.google.gms:google-services:4.3.10")
//        classpath("com.android.tools.build:gradle:7.3.1")
//        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

plugins {
    id("com.apollographql.apollo3").version("3.2.2").apply(false)
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
//    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin").version("2.0.1").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}


