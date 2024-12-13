plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
}

tasks.register("build") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":build"))
    }
}

tasks.register("clean") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":clean"))
    }
}
