import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    compileOnly(libs.android.gradle.api)
    implementation(libs.kotlinx.serialization.json)
    detektPlugins(libs.bundles.detekt)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }
}

gradlePlugin {
    website.set("https://github.com/Commencis/secrets-vault-plugin")
    vcsUrl.set("https://github.com/Commencis/secrets-vault-plugin.git")
    plugins {
        create("SecretsVaultPlugin") {
            id = "com.commencis.secretsvaultplugin"
            displayName = "Secrets Vault Plugin"
            description = "This plugin allows any Android developer" +
                    " to deeply hide secrets in its project to prevent credentials harvesting."
            implementationClass = "com.commencis.secretsvaultplugin.SecretsVaultPlugin"
            tags.set(listOf("android", "keep", "hide", "secret", "key", "obfuscate"))
        }
    }
}

configure<DetektExtension> {
    source = project.files("src/main/kotlin")
    buildUponDefaultConfig = true
    allRules = false
    config = files("../.detekt/config.yml")
    baseline = file("./.detekt/baseline.xml")
}

group = "com.commencis.secretsvaultplugin"
version = "0.1.4"
