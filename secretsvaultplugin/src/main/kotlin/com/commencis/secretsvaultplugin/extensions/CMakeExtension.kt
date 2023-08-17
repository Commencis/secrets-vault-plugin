package com.commencis.secretsvaultplugin.extensions

import org.gradle.api.provider.Property

/**
 * Provides extension properties for the generated CMakeList, allowing configurations to be set.
 *
 * @property projectName The name of the CMAKE project.
 * If not specified, the name of the module to which the plugin is applied will be used.
 * @property version The version of CMake. If not specified, [DEFAULT_CMAKE_VERSION] will be used.
 */
internal interface CMakeExtension {
    val projectName: Property<String>
    val version: Property<String>
}
