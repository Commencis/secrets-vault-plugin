import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.commencis.secretsvaultplugin.sampleapp"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.commencis.secretsvaultplugin.sampleapp"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/debug.jks")
            storePassword = "android"
            keyAlias = "debug"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("keystore/release.jks")
            storePassword = "android"
            keyAlias = "release"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    flavorDimensions.add("stage")
    productFlavors {
        create("dev") {
            dimension = "stage"
        }
        create("prod") {
            dimension = "stage"
        }
    }

    kotlinOptions {
        jvmTarget = JvmTarget.JVM_11.target
    }
}
