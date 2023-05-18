package com.commencis.secretsvaultplugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction

// Tasks
private const val TASK_GROUP = "Secrets Vault"
private const val TASK_UNZIP_SECRETS_VAULT = "unzipSecretsVault"

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
        project.tasks.create(
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
        ).apply {
            group = TASK_GROUP
            this.description = "Unzip secrets vault plugin into temp directory"
        }
    }
}
