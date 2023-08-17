package com.commencis.secretsvaultplugin

import com.commencis.secretsvaultplugin.extensions.SecretsVaultExtension
import com.commencis.secretsvaultplugin.utils.Utils
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

// Tasks
private const val TASK_GROUP = "secrets vault"
private const val TASK_UNZIP_SECRETS_VAULT = "unzipSecretsVault"
private const val TASK_KEEP_SECRETS_FROM_JSON_FILE = "keepSecrets"

// Extensions
private const val EXTENSION_NAME = "secretsVault"

// Defaults
private const val DEFAULT_CMAKE_VERSION = "3.4.1"

/**
 * Represents the default file name for the secrets file.
 */
private const val DEFAULT_SECRETS_FILE_NAME = "secrets.json"

/**
 * Main class of the Secrets Vault Plugin.
 * Provides the gradle tasks for hiding secrets in a json file.
 */
internal class SecretsVaultPlugin : Plugin<Project> {

    /**
     * Applies the plugin to a given [Project].
     * @param project the project to which the plugin should be applied.
     */
    override fun apply(project: Project) {
        project.extensions.create(EXTENSION_NAME, SecretsVaultExtension::class.java).apply {
            obfuscationKey.convention(Utils.generateObfuscationKey())
            secretsFile.convention(project.file(DEFAULT_SECRETS_FILE_NAME))
            appSignatures.convention(emptyList())
            cmake.cmakeProjectName.convention(project.name)
            cmake.cmakeVersion.convention(DEFAULT_CMAKE_VERSION)
        }

        /**
         * Create a gradle task to unzip the plugin into a temporary directory.
         */
        val unzipTaskProvider = project.tasks.register(TASK_UNZIP_SECRETS_VAULT, Copy::class.java) {
            group = TASK_GROUP
            description = "Unzip secrets vault plugin into temp directory"
            // Get the location of the current class's compiled code
            val classCodeLocation =
                this@SecretsVaultPlugin.javaClass.protectionDomain.codeSource.location.toExternalForm()

            // Create a zip tree from the location and copy it into a temporary folder
            from(project.zipTree(classCodeLocation)) {
                include("**/cpp/**")
                include("**/kotlin/**")
            }
            into(project.layout.buildDirectory.dir("intermediates/secrets_vault_plugin"))
        }

        /**
         * Create a gradle task to keep secrets from a json file.
         */
        project.tasks.register(TASK_KEEP_SECRETS_FROM_JSON_FILE, KeepSecretsTask::class.java).configure {
            group = TASK_GROUP
            description = "Re-generate and obfuscate keys from the json file and add it to your Android project"
            pluginSourceFolder.set(unzipTaskProvider.map { task -> task.destinationDir })
            json.set(Json { encodeDefaults = true })
        }
    }
}
