plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.commencis.secretsvaultplugin")
}

android {
    namespace = "com.commencis.secretsvaultplugin.sampleapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.commencis.secretsvaultplugin.sampleapp"
        minSdk = 23
        targetSdk = 36
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

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    flavorDimensions.add("stage")
    productFlavors {
        create("dev") {
            dimension = "stage"
            externalNativeBuild {
                cmake {
                    arguments("-DsourceSet=dev")
                }
            }
        }
        create("prod") {
            dimension = "stage"
            externalNativeBuild {
                cmake {
                    arguments("-DsourceSet=prod")
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }
}

secretsVault {
    appSignatures.set(
        listOf(
            "1A:92:D7:89:8F:16:C4:D3:46:E2:6D:C5:0C:2F:42:B0", // keystore/debug.kjs
            "45:4E:FD:58:87:C2:27:D2:5E:12:F4:C6:7F:CA:53:10", // keystore/release.kjs
        )
    )
    obfuscationKey.set("chEYKrGb5PJx0I09oa1mlEuXE5FxPjX2")
    cmake {
        version.set("3.17")
    }
}

dependencies {
    implementation(libs.androidx.activityKtx)
}
