pluginManagement {
    includeBuild("secretsvaultplugin") {
        name = "secretsvaultplugin_included"
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Secrets Vault Plugin"

include(":sampleapp")
include(":secretsvaultplugin")
