plugins {
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.gradle.publish).apply(false)
    alias(libs.plugins.detekt).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)

    // Sample App
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
}
