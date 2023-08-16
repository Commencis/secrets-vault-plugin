package com.commencis.secretsvaultplugin.extensions

import com.commencis.secretsvaultplugin.utils.EMPTY_STRING
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

/**
 * Provides extension properties for the SecretVault, allowing configurations to be set.
 *
 * @property obfuscationKey The obfuscation key, the default value is a random alphanumeric string
 * of length defined by [OBFUSCATION_KEY_LENGTH].
 * @property secretsFile The name of the secrets file.
 * If not specified, the default name [DEFAULT_SECRETS_FILE_NAME] is used.
 * @property appSignatures A list of application signatures.  If not specified, an empty list is used.
 * @property packageName The name of the package. If not specified, an empty string [EMPTY_STRING] is used.
 * @property cmakeProjectName The name of the CMAKE project.
 * If not specified, the name of the module to which the plugin is applied will be used.
 * @property cmakeVersion The version of CMake. If not specified, [DEFAULT_CMAKE_VERSION] will be used.
 */
internal interface SecretsVaultExtension {
    val obfuscationKey: Property<String>
    val secretsFile: Property<File>
    val appSignatures: ListProperty<String>
    val packageName: Property<String>
    val cmakeProjectName: Property<String>
    val cmakeVersion: Property<String>
}
