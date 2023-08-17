package com.commencis.secretsvaultplugin.extensions

import org.gradle.api.provider.Property

/**
 * Provides extension properties for the generated CMakeList, allowing configurations to be set.
 *
 * @property cmakeProjectName The name of the CMAKE project.
 * If not specified, the name of the module to which the plugin is applied will be used.
 * @property cmakeVersion The version of CMake. If not specified, [DEFAULT_CMAKE_VERSION] will be used.
 */
internal interface CMakeExtension {
    val cmakeProjectName: Property<String>
    val cmakeVersion: Property<String>
}
