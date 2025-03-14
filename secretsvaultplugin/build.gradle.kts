import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
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
version = "0.1.3"
