package com.commencis.secretsvaultplugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction

// Tasks
private const val TASK_GROUP = "secrets vault"
private const val TASK_UNZIP_SECRETS_VAULT = "unzipSecretsVault"
private const val TASK_KEEP_SECRETS_FROM_JSON_FILE = "keepSecrets"

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
        /**
         * Temporary folder for storing the secrets
         */
        val tempFolder = "${project.buildDir}/secrets-vault-temp"

        /**
         * Create a gradle task to unzip the plugin into a temporary directory.
         */
        project.tasks.register(
            TASK_UNZIP_SECRETS_VAULT,
            Copy::class.java,
            object : Action<Copy> {
                @TaskAction
                override fun execute(copy: Copy) {
                    // Get the location of the current class's compiled code
                    val classCodeLocation = javaClass.protectionDomain.codeSource.location.toExternalForm()

                    // Create a zip tree from the location and copy it into a temporary folder
                    copy.from(project.zipTree(classCodeLocation))
                    copy.into(tempFolder)

                    println("Unzipped jar to $tempFolder")
                }
            }
        ).configure {
            group = TASK_GROUP
            description = "Unzip secrets vault plugin into temp directory"
        }

        /**
         * Create a gradle task to keep secrets from a json file.
         */
        project.tasks.register(
            TASK_KEEP_SECRETS_FROM_JSON_FILE,
            KeepSecretsTask::class.java,
        ).configure {
            group = TASK_GROUP
            description = "Re-generate and obfuscate keys from the json file and add it to your Android project"
            dependsOn(TASK_UNZIP_SECRETS_VAULT)
        }
    }
}
