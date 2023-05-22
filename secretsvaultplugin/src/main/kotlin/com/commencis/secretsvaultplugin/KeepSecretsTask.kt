package com.commencis.secretsvaultplugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.commencis.secretsvaultplugin.utils.CodeGenerator
import com.commencis.secretsvaultplugin.utils.Utils
import com.commencis.secretsvaultplugin.utils.capitalize
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

private const val SOURCE_SET_TEMPLATE = "src/%1\$s/%2\$s/"
private const val PACKAGE_PLACEHOLDER = "YOUR_PACKAGE_NAME"
private const val SECRETS_FILE_PLACEHOLDER = "SECRETS_FILE_PREFIX"
private const val SECRETS_CLASS_NAME_PLACEHOLDER = "SECRETS_CLASS_NAME_PREFIX"
private const val OBFUSCATION_KEY_PLACEHOLDER = "OBFUSCATION_KEY_PLACEHOLDER"
private const val KOTLIN_FILE_NAME = "Secrets.kt"
private const val SECRETS_CPP_FILE_NAME = "secrets.cpp"
private const val C_MAKE_LISTS_FILE_NAME = "CMakeLists.txt"
private const val MAIN_SOURCE_SET_NAME = "main"

private const val OBFUSCATION_KEY_LENGTH = 32

// Properties
private const val PROP_OBFUSCATION_KEY = "obfuscationKey"
private const val PROP_FILE_NAME = "fileName"
private const val PROP_APP_SIGNATURES = "appSignatures"
private const val PROP_PACKAGE_NAME = "packageName"

// Sample usage
private const val SAMPLE_FROM_PROPS = "-P$PROP_FILE_NAME=credentials.json"

// Ansi Colors
private const val ANSI_COLOR_RESET = "\u001B[0m"
private const val ANSI_COLOR_GREEN = "\u001B[32m"
private const val ANSI_COLOR_YELLOW = "\u001B[33m"

/**
 * A Gradle task to keep secrets from a json file.
 * This task removes all previously secured secret keys in your project and
 * replaces them with obfuscated keys from the provided json file.
 */
@Suppress("TooManyFunctions")
internal abstract class KeepSecretsTask : DefaultTask() {

    /**
     * Map containing the secrets
     */
    private var secretsMap: Map<String, List<Secret>>? = null

    /**
     * Temporary folder for storing the secrets
     */
    private val tempFolder = "${project.buildDir}/secrets-vault-temp"

    /**
     * Retrieves or generates the obfuscation key.
     * Checks if the project has the obfuscation key property.
     * If the property exists, it will be used as the obfuscation key.
     * Otherwise, a new obfuscation key is generated.
     *
     * @return The obfuscation key as a String.
     */
    private val obfuscationKey by lazy {
        if (project.hasProperty(PROP_OBFUSCATION_KEY)) {
            project.property(PROP_OBFUSCATION_KEY) as String
        } else {
            generateObfuscationKey()
        }
    }

    /**
     * The main task action which is responsible for keeping secrets
     */
    @TaskAction
    fun keepSecrets() {
        val secretsFile = getSecretsFile()
        logger.lifecycle("Generating secrets from file: ${secretsFile.path}")
        initSecretsFromFile(secretsFile = secretsFile)
        val secrets = secretsMap ?: return
        copyCMakeListsFile(secrets.keys)
        secrets.forEach { (flavour, secretList) ->
            keepSecrets(flavour, secretList)
        }
    }

    /**
     * Get the package name of the Android app on which this plugin is used
     *
     * If the project has a property named [PROP_PACKAGE_NAME], it will be used as the package name.
     * Otherwise, the function will attempt to get the applicationId from the ApplicationExtension of the project.
     *
     * @return the package name of the Android app
     */
    private fun getAppPackageName(): String {
        return if (project.hasProperty(PROP_PACKAGE_NAME)) {
            project.property(PROP_PACKAGE_NAME) as String
        } else {
            val commonExtension = project.extensions.getByType(CommonExtension::class.java)
            (commonExtension as? ApplicationExtension)?.defaultConfig?.applicationId ?: ""
        }
    }

    /**
     * Get app signatures param from command line
     * @return the list of app signatures if the project has a property named [PROP_APP_SIGNATURES] or null
     */
    private fun getAppSignatures(): List<String>? {
        return if (project.hasProperty(PROP_APP_SIGNATURES)) {
            (project.property(PROP_APP_SIGNATURES) as String).split(",")
        } else {
            null
        }
    }

    /**
     * Get json file to hide secrets from command line
     *
     * @throws IllegalArgumentException if no props are found in the project or if the file is not a valid file
     * @return the json file
     */
    @Throws(IllegalArgumentException::class)
    private fun getSecretsFile(): File {
        require(project.hasProperty(PROP_FILE_NAME)) {
            "Please provide json file that holds secrets! Use: $SAMPLE_FROM_PROPS"
        }
        val fileName = project.property(PROP_FILE_NAME) as String
        val secretsFile = File(project.rootDir, fileName)
        require(secretsFile.exists() && secretsFile.isFile) {
            "${secretsFile.name} does not exist or is not a valid file!"
        }
        return secretsFile
    }

    /**
     * Initialize the secrets from the provided file
     *
     * @param secretsFile the file from which the secrets will be read
     */
    private fun initSecretsFromFile(secretsFile: File) {
        val content = secretsFile.readText(Charsets.UTF_8)
        runCatching {
            val json = Json { encodeDefaults = true }
            secretsMap = json.decodeFromString<Array<Secret>>(content).groupBy { it.flavour }
        }.onFailure { throwable ->
            logger.error(
                """
                    |Error loading credentials from file due to: ${throwable.message}
                    |Please ensure that your credentials file follows this format:
                    |[
                    |    { "key": "apiKey1", "value": "API_VALUE_1_DEVELOPMENT", "flavour": "dev" },
                    |    { "key": "apiKey1", "value": "API_VALUE_1_PRODUCTION", "flavour": "prod" },
                    |    { "key": "apiKey2", "value": "API_VALUE_2_GENERAL" }
                    |]
                    |Each entry should include a 'key' and 'value'. 
                    |If the key is specific to a particular environment, you can also include a 'flavour'.
                """.trimMargin(),
            )
        }
    }

    /**
     * Get the destination file for the CPP code
     *
     * @param flavour the build flavour for which the destination file should be returned
     * @param fileName the name of the file
     * @return the destination file for the CPP code
     */
    private fun getCppDestination(flavour: String, fileName: String): File {
        return project.file(SOURCE_SET_TEMPLATE.format(flavour, "cpp") + "$fileName")
    }

    /**
     * Get the destination file for the Kotlin code
     *
     * @param flavour the build flavour for which the destination file should be returned
     * @param fileName the name of the file
     * @return the destination file for the Kotlin code
     */
    private fun getKotlinDestination(flavour: String, fileName: String): File {
        val javaPath = SOURCE_SET_TEMPLATE.format(flavour, "java")
        val kotlinPath = SOURCE_SET_TEMPLATE.format(flavour, "kotlin")
        val basePath = if (project.file(javaPath).exists()) javaPath else kotlinPath
        val packagePath = getAppPackageName().split(".").joinToString(File.separator)
        val fullPath = basePath + packagePath

        val directory = project.file(fullPath)
        if (directory.exists().not()) {
            logger.lifecycle("Directory $fullPath does not exist in the project, creating it.")
            directory.mkdirs()
        }

        return File(directory, fileName)
    }

    /**
     * Copies CPP files to the appropriate destination for the specified build flavour
     *
     * @param flavour the build flavour for which the files should be copied
     */
    private fun copyCppFiles(flavour: String) {
        runCatching {
            project.file("$tempFolder/cpp/").listFiles()?.forEach { file ->
                if (file.name == C_MAKE_LISTS_FILE_NAME) {
                    return@forEach
                }
                val appSignatures = getAppSignatures()?.map { appSignature ->
                    Utils.encodeSecret(appSignature, obfuscationKey)
                }
                var text = file.readText(Charset.defaultCharset())
                text = text.replace(OBFUSCATION_KEY_PLACEHOLDER, obfuscationKey)
                    .replace(
                        CodeGenerator.CHECK_APP_SIGNATURE_PLACEHOLDER,
                        appSignatures?.let { appSignatureList ->
                            CodeGenerator.getAppSignatureCheck(appSignatureList)
                        }.orEmpty()
                    )
                val destination = getCppDestination(flavour = flavour, fileName = file.name)
                destination.parentFile?.takeIf { it.exists().not() }?.mkdirs()
                destination.takeIf { it.exists().not() }?.createNewFile()
                destination.writeText(text)
            }
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying CPP files: ${throwable.message}")
            }
        }
    }

    /**
     * Copies the CMakeLists.txt file to the appropriate destination for all the provided build flavours
     *
     * @param flavours the set of build flavours for which the file should be copied
     */
    private fun copyCMakeListsFile(flavours: Set<String>) {
        runCatching {
            project.file("$tempFolder/cpp/").listFiles()?.forEach { file ->
                if (file.name != C_MAKE_LISTS_FILE_NAME) {
                    return@forEach
                }
                var text = file.readText(Charset.defaultCharset())
                flavours.forEachIndexed { index, flavour ->
                    if (flavour == MAIN_SOURCE_SET_NAME) return@forEachIndexed
                    text += CodeGenerator.getCMakeListsCode(flavour = flavour, index == 0)
                }
                if (flavours.size > 1) {
                    text += "endif()\n"
                }
                val destination = getCppDestination(MAIN_SOURCE_SET_NAME, fileName = file.name)
                destination.parentFile?.takeIf { it.exists().not() }?.mkdirs()
                destination.takeIf { it.exists().not() }?.createNewFile()
                destination.writeText(text)
            }
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying CMakeLists file: ${throwable.message}")
            }
        }
    }

    private fun copyKotlinFile(flavour: String) {
        runCatching {
            project.file("$tempFolder/kotlin/").listFiles()?.forEach { file ->
                var text = file.readText(Charset.defaultCharset())
                val secretsFilePrefix = if (flavour == MAIN_SOURCE_SET_NAME) MAIN_SOURCE_SET_NAME else ""
                text = text.replace(PACKAGE_PLACEHOLDER, getAppPackageName())
                    .replace(SECRETS_FILE_PLACEHOLDER, secretsFilePrefix)
                    .replace(SECRETS_CLASS_NAME_PLACEHOLDER, secretsFilePrefix.capitalize())
                val destination = getKotlinDestination(
                    flavour = flavour,
                    fileName = secretsFilePrefix.capitalize() + file.name,
                )
                destination.parentFile?.takeIf { it.exists().not() }?.mkdirs()
                destination.takeIf { it.exists().not() }?.createNewFile()
                destination.writeText(text)
            }
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying Kotlin file: ${throwable.message}")
            }
        }
    }

    /**
     * Keeps the provided secrets for the specified build flavour
     *
     * @param flavour the build flavour for which the secrets should be hidden
     * @param secrets the list of secrets to be hidden
     */
    private fun keepSecrets(flavour: String, secrets: List<Secret>) {
        if (secrets.isEmpty()) {
            logWarning("No secrets to hide for the flavour $flavour.")
            return
        }

        copyCppFiles(flavour)
        copyKotlinFile(flavour)

        val secretsFilePrefix = if (flavour == MAIN_SOURCE_SET_NAME) MAIN_SOURCE_SET_NAME else ""
        val secretsKotlin = getKotlinDestination(
            flavour = flavour,
            fileName = secretsFilePrefix.capitalize() + KOTLIN_FILE_NAME
        )

        // Append Kotlin code
        val kotlinText = secretsKotlin.readText(Charset.defaultCharset()).substringBeforeLast('}')
        secretsKotlin.writeText(kotlinText, Charset.defaultCharset())
        secretsKotlin.appendText(secrets.joinToString("") { CodeGenerator.getKotlinCode(it.key) })
        secretsKotlin.appendText("}\n")

        // Append CPP code
        var kotlinPackage = Utils.getKotlinFilePackage(secretsKotlin)
        if (kotlinPackage.isNullOrEmpty()) {
            logWarning("Empty package in $KOTLIN_FILE_NAME")
            kotlinPackage = getAppPackageName()
        }

        val secretsCpp = getCppDestination(flavour = flavour, fileName = SECRETS_CPP_FILE_NAME)
        secrets.forEach { secret ->
            val (key, value) = secret
            val obfuscatedValue = Utils.encodeSecret(value, obfuscationKey)
            val cppText = secretsCpp.readText(Charset.defaultCharset())
            if (cppText.contains(obfuscatedValue)) {
                logWarning("Key already added in C++ !")
                return@forEach
            }
            val cppKeyName = Utils.getCppName(key)
            secretsCpp.appendText(CodeGenerator.getCppCode(kotlinPackage, cppKeyName, obfuscatedValue, flavour))
        }
        val secretsPrefix = if (flavour == MAIN_SOURCE_SET_NAME) MAIN_SOURCE_SET_NAME.capitalize() else ""
        logSuccess(
            "You can now get your secret key for flavour {} by calling : {}Secrets().getYourSecretKeyName()",
            flavour,
            secretsPrefix,
        )
    }

    /**
     * Generates a random alphanumeric string of length 32 for obfuscation purposes.
     *
     * This function creates a 32-character string that contains random alphanumeric
     * characters (both lowercase and uppercase letters, and digits).
     *
     * @return A random alphanumeric string of length 32.
     */
    private fun generateObfuscationKey(): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..OBFUSCATION_KEY_LENGTH)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Logs a warning message to the console using a specified color for warnings (yellow).
     *
     * @param message The message to be logged.
     * @param parameters Additional parameters to be included in the log message.
     * These parameters will replace the placeholders in the message string.
     *
     * Note: Use {} as placeholders in the message for parameters.
     * The message and the parameters will be concatenated.
     * The color will be reset at the end.
     */
    private fun logWarning(message: String, vararg parameters: String) {
        logger.lifecycle(
            "{}$message{}",
            ANSI_COLOR_YELLOW,
            *parameters,
            ANSI_COLOR_RESET,
        )
    }

    /**
     * Logs a success message to the console using a specified color for success (green).
     *
     * @param message The message to be logged.
     * @param parameters Additional parameters to be included in the log message.
     * These parameters will replace the placeholders in the message string.
     *
     * Note: Use {} as placeholders in the message for parameters.
     * The message and the parameters will be concatenated.
     * The color will be reset at the end.
     */
    private fun logSuccess(message: String, vararg parameters: String) {
        logger.lifecycle(
            "{}$message{}",
            ANSI_COLOR_GREEN,
            *parameters,
            ANSI_COLOR_RESET,
        )
    }
}