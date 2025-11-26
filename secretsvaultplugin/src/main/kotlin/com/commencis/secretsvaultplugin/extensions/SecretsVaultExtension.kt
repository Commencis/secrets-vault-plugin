package com.commencis.secretsvaultplugin.extensions

import com.commencis.secretsvaultplugin.utils.EMPTY_STRING
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Provides extension properties for the SecretVault, allowing configurations to be set.
 *
 * @property obfuscationKey The obfuscation key, the default value is a random alphanumeric string
 * of length defined by [OBFUSCATION_KEY_LENGTH].
 * @property secretsFile The name of the secrets file.
 * If not specified, the default name [DEFAULT_SECRETS_FILE_NAME] is used.
 * @property sourceSetSecretsMappingFile The name of the map file that maps source sets to their respective
 * secrets files. If not specified, the default mapping is used. For all source sets except the main source set,
 * secrets files are named Secrets.kt.
 * @property appSignatures A list of application signatures.  If not specified, an empty list is used.
 * @property packageName The name of the package. If not specified, an empty string [EMPTY_STRING] is used.
 * @property makeInjectable Specifies whether the generated Kotlin class should include the `@Inject`
 * annotation for its constructor. When set to true, the class will be made injectable, facilitating its integration
 * with dependency injection frameworks such as Hilt and Dagger. If not specified, the default value is `false`.
 * @property cmake The CMake related configurations for the SecretVault. This includes properties like
 * the project name and version of CMake being used. Refer to [CMakeExtension] for detailed properties.
 */
interface SecretsVaultExtension {
    val obfuscationKey: Property<String>
    val secretsFile: RegularFileProperty
    val sourceSetSecretsMappingFile: RegularFileProperty
    val appSignatures: ListProperty<String>
    val packageName: Property<String>
    val makeInjectable: Property<Boolean>
    val cmake: Property<CMakeExtension>
}
