package com.commencis.secretsvaultplugin

import com.android.build.api.dsl.CommonExtension
import com.commencis.secretsvaultplugin.extensions.SecretsVaultExtension
import com.commencis.secretsvaultplugin.utils.CHECK_APP_SIGNATURE_PLACEHOLDER
import com.commencis.secretsvaultplugin.utils.CodeGenerator
import com.commencis.secretsvaultplugin.utils.EMPTY_STRING
import com.commencis.secretsvaultplugin.utils.Utils
import com.commencis.secretsvaultplugin.utils.capitalize
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

internal const val MAIN_SOURCE_SET_NAME = "main"

private const val SOURCE_SET_TEMPLATE = "src/%1\$s/%2\$s/"
private const val PACKAGE_PLACEHOLDER = "YOUR_PACKAGE_NAME"
private const val SECRETS_CLASS_NAME_PLACEHOLDER = "SECRETS_CLASS_NAME"
private const val OBFUSCATION_KEY_PLACEHOLDER = "OBFUSCATION_KEY_PLACEHOLDER"
private const val PROJECT_NAME_PLACEHOLDER = "PROJECT_NAME_PLACEHOLDER"
private const val CMAKE_VERSION_PLACEHOLDER = "CMAKE_VERSION_PLACEHOLDER"
private const val EXTERNAL_METHODS_PLACEHOLDER = "EXTERNAL_METHODS_PLACEHOLDER"
private const val NATIVE_FILE_NAME_PLACEHOLDER = "NATIVE_FILE_NAME_PLACEHOLDER"
private const val COMMON_FOLDER_PATH_PREFIX_PLACEHOLDER = "COMMON_FOLDER_PATH_PREFIX_PLACEHOLDER"
private const val COMMON_FOLDER_PATH_PREFIX = "../../main/cpp/"
private const val KOTLIN_FILE_NAME_SUFFIX = ".kt"
private const val SECRETS_CPP_FILE_NAME = "secrets.cpp"
private const val C_MAKE_LISTS_FILE_NAME = "CMakeLists.txt"
private const val SECRETS_UTIL_CPP_FILE_NAME = "secrets_util.cpp"
private const val MAIN_SOURCE_SET_SECRETS_FILE_NAME = "MainSecrets"
private const val DEFAULT_SECRETS_FILE_NAME = "Secrets"
private const val TEMP_KOTLIN_INJECTABLE_FILE_NAME = "SecretsInjectable.kt"
private const val TEMP_KOTLIN_NOT_INJECTABLE_FILE_NAME = "Secrets.kt"
private const val DEFAULT_CMAKE_ARGUMENT_NAME = "sourceSet"
private const val JVM_NAME_PREFIX = "a"

// Ansi Colors
private const val ANSI_COLOR_RESET = "\u001B[0m"
private const val ANSI_COLOR_GREEN = "\u001B[32m"
private const val ANSI_COLOR_YELLOW = "\u001B[33m"

/**
 * A Gradle task to keep secrets from a json file.
 * This task removes all previously secured secret keys in your project and
 * replaces them with obfuscated keys from the provided json file.
 */
@Suppress("TooManyFunctions", "UnnecessaryAbstractClass")
internal abstract class KeepSecretsTask : DefaultTask() {

    /**
     * Map containing the secrets
     */
    private var secretsMap: Map<SecretsSourceSet, List<Secret>>? = null

    /**
     * Map containing the source sets and their respective secrets file names
     */
    private var sourceSetToSecretFileMap: Map<SecretsSourceSet, Pair<SecretsFileName, CMakeArgument>>? = null

    /**
     * Plugin source folder that has the cpp and kotlin files
     */
    @get:InputDirectory
    abstract val pluginSourceFolder: Property<File>

    /**
     * Represents the JSON property of the class it's abstracted in.
     */
    @get:Internal
    abstract val json: Property<Json>

    /**
     * Provides lazy access to the [SecretsVaultExtension] of the project.
     */
    private val secretsVaultExtension by lazy {
        project.extensions.getByType(SecretsVaultExtension::class.java)
    }

    /**
     * Provides lazy access to the [CMakeExtension] of the project.
     */
    private val cMakeExtension by lazy {
        secretsVaultExtension.cmake.get()
    }

    /**
     * Get the package name of the module on which this plugin is used
     *
     * The function will first attempt to get the package name from the [SecretsVaultExtension].
     * If it's not provided (i.e., it's an empty string), the function will attempt to get the namespace
     * from the [CommonExtension] of the project.
     */
    private val packageName: String by lazy {
        secretsVaultExtension.packageName.getOrElse(EMPTY_STRING).ifEmpty {
            project.extensions.getByType(CommonExtension::class.java).namespace.orEmpty()
        }
    }

    /**
     * Lazily initialized instance of [CodeGenerator].
     */
    private val codeGenerator by lazy { CodeGenerator() }

    /**
     * The main task action which is responsible for keeping secrets
     */
    @TaskAction
    fun keepSecrets() {
        val secretsFile = getSecretsFile()
        logger.lifecycle("Generating secrets from file: ${secretsFile.path}")
        initSecretsFromFile(secretsFile = secretsFile)
        getSourceSetToSecretMappingFile()?.let { mappingFile ->
            initSourceSetToSecretFileMap(mappingFile = mappingFile)
        }
        val secrets = secretsMap ?: return
        copyCMakeListsFile(secrets.keys)
        copyCommonCppFiles()
        secrets.forEach { (sourceSet, secretList) ->
            keepSecrets(sourceSet, secretList)
        }
    }

    /**
     * Get json file to hide secrets
     *
     * @throws IllegalArgumentException if the file is not a valid file
     * @return the json file
     */
    @Throws(IllegalArgumentException::class)
    private fun getSecretsFile(): File {
        val secretsFile = secretsVaultExtension.secretsFile.get()
        require(secretsFile.exists() && secretsFile.isFile) {
            "${secretsFile.name} does not exist or is not a valid file!"
        }
        return secretsFile
    }

    /**
     * Get json file to map source sets to secrets Kotlin file name
     *
     * @throws IllegalArgumentException if the file is not a valid file
     * @return the json file
     */
    @Throws(IllegalArgumentException::class)
    private fun getSourceSetToSecretMappingFile(): File? {
        val mappingFile = secretsVaultExtension.sourceSetSecretsMappingFile.orNull
        if (mappingFile != null) {
            require(mappingFile.exists() && mappingFile.isFile) {
                "${mappingFile.name} does not exist or is not a valid file!"
            }
        }
        return mappingFile
    }

    /**
     * Initialize the secrets from the provided file
     *
     * @param secretsFile the file from which the secrets will be read
     */
    private fun initSecretsFromFile(secretsFile: File) {
        val content = secretsFile.readText(Charsets.UTF_8)
        runCatching {
            secretsMap = json.get().decodeFromString<Secrets>(content).secrets.groupBy { it.sourceSet }
        }.onFailure { throwable ->
            logger.error(
                """
                    |Error loading credentials from file due to: ${throwable.message}
                    |Please ensure that your credentials file follows this format:
                    |[
                    |    { "key": "apiKey1", "value": "API_VALUE_1_DEVELOPMENT", "sourceSet": "dev" },
                    |    { "key": "apiKey1", "value": "API_VALUE_1_PRODUCTION", "sourceSet": "prod" },
                    |    { "key": "apiKey2", "value": "API_VALUE_2_GENERAL" }
                    |]
                    |Each entry should include a 'key' and 'value'. 
                    |If the key is specific to a particular environment, you can also include a 'sourceSet'.
                """.trimMargin(),
            )
        }
    }

    /**
     * Initialize the source set to secrets mapping from the provided file.
     *
     * @param mappingFile the file from which the source set to secrets mapping will be read.
     */
    private fun initSourceSetToSecretFileMap(mappingFile: File) {
        val content = mappingFile.readText(Charsets.UTF_8)
        runCatching {
            sourceSetToSecretFileMap = json.get().decodeFromString<SourceSetToSecretFileMappingArray>(content).toMap()
            if (sourceSetToSecretFileMap?.any { it.value.first.name == MAIN_SOURCE_SET_SECRETS_FILE_NAME } == true) {
                logger.error(
                    """
                        |The 'MainSecrets' file name is reserved for the main source set.
                        |Please rename the 'MainSecrets' file name to something else.
                    """.trimMargin(),
                )
            }
        }.onFailure { throwable ->
            logger.error(
                """
                    |Error loading secrets file mapping from file due to: ${throwable.message}
                    |Please ensure that your mapping file follows this format:
                    |[
                    |  {
                    |    "secretsFileName" : "StageSecrets",
                    |    "sourceSets" : [
                    |      "dev",
                    |      "qa",
                    |      "prod",
                    |    ]
                    |  }
                    |]
                    |Each entry should include a 'secretsFileName' and 'sourceSets' array.
                """.trimMargin(),
            )
        }
    }

    /**
     * Get the destination file for the CPP code
     *
     * @param sourceSet the source set or build flavor for which the destination file should be returned
     * @param fileName the name of the file
     * @param pathSuffix the suffix to be appended to the path before the file name
     * @return the destination file for the CPP code
     */
    private fun getCppDestination(
        sourceSet: SecretsSourceSet,
        fileName: String,
        pathSuffix: String = EMPTY_STRING,
    ): File {
        return project.file(SOURCE_SET_TEMPLATE.format(sourceSet.sourceSet, "cpp") + pathSuffix + fileName)
    }

    /**
     * Get the destination file for the Kotlin code
     *
     * @param sourceSet the source set or build flavor for which the destination file should be returned
     * @param fileName the name of the file
     * @return the destination file for the Kotlin code
     */
    private fun getKotlinDestination(sourceSet: SecretsSourceSet, fileName: String): File {
        val kotlinPath = SOURCE_SET_TEMPLATE.format(sourceSet.sourceSet, "kotlin")
        val javaPath = SOURCE_SET_TEMPLATE.format(sourceSet.sourceSet, "java")
        val basePath = kotlinPath.takeIf { project.file(kotlinPath).exists() } ?: javaPath
        val packagePath = packageName.replace(".", File.separator)
        val fullPath = basePath + packagePath

        val directory = project.file(fullPath)
        if (directory.exists().not()) {
            logger.lifecycle("Directory $fullPath does not exist in the project, creating it.")
            directory.mkdirs()
        }

        return File(directory, fileName)
    }

    /**
     * Copies common CPP files to the appropriate destination of the main source set
     */
    private fun copyCommonCppFiles() {
        runCatching {
            val appSignaturesCodeBlock = secretsVaultExtension.appSignatures.get().map { appSignature ->
                Utils.encodeSecret(
                    secretKey = appSignature.replace(":", EMPTY_STRING),
                    obfuscationKey = secretsVaultExtension.obfuscationKey.get(),
                )
            }.let { encodedAppSignatures ->
                codeGenerator.getAppSignatureCheck(encodedAppSignatures)
            }
            project.file("${pluginSourceFolder.get().path}/cpp/common/").listFiles()?.forEach { file ->
                var text = file.readText(Charset.defaultCharset())
                if (file.name == SECRETS_UTIL_CPP_FILE_NAME) {
                    text = text.replace(OBFUSCATION_KEY_PLACEHOLDER, secretsVaultExtension.obfuscationKey.get())
                        .replace(CHECK_APP_SIGNATURE_PLACEHOLDER, appSignaturesCodeBlock)
                }
                val destination = getCppDestination(
                    sourceSet = SecretsSourceSet(MAIN_SOURCE_SET_NAME),
                    fileName = file.name,
                    pathSuffix = "common/",
                )
                writeTextToFile(destination, text)
            }
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying CPP files: ${throwable.message}")
            }
        }
    }

    /**
     * Copies [SECRETS_CPP_FILE_NAME] file to the appropriate destination for the specified source set
     *
     * @param sourceSet the source set or build flavor for which the file should be copied
     */
    private fun copySecretCppFile(sourceSet: SecretsSourceSet) {
        runCatching {
            val secretsFile = project.file("${pluginSourceFolder.get().path}/cpp/$SECRETS_CPP_FILE_NAME")
            val text = secretsFile.readText(Charset.defaultCharset()).replace(
                oldValue = COMMON_FOLDER_PATH_PREFIX_PLACEHOLDER,
                newValue = if (sourceSet == SecretsSourceSet(MAIN_SOURCE_SET_NAME)) {
                    EMPTY_STRING
                } else {
                    COMMON_FOLDER_PATH_PREFIX
                },
            )
            val destination = getCppDestination(sourceSet = sourceSet, fileName = SECRETS_CPP_FILE_NAME)
            writeTextToFile(destination, text)
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying $SECRETS_CPP_FILE_NAME file: ${throwable.message}")
            }
        }
    }

    /**
     * Copies the CMakeLists.txt file to the appropriate destination for all the provided source sets
     *
     * @param sourceSets the set of build source sets or flavors for which the file should be copied
     */
    private fun copyCMakeListsFile(sourceSets: Set<SecretsSourceSet>) {
        runCatching {
            val mainSourceSet = SecretsSourceSet(MAIN_SOURCE_SET_NAME)
            val file = project.file("${pluginSourceFolder.get().path}/cpp/$C_MAKE_LISTS_FILE_NAME")
            val textBuilder = StringBuilder(
                file.readText(Charset.defaultCharset())
                    .replace(PROJECT_NAME_PLACEHOLDER, cMakeExtension.projectName.get())
                    .replace(CMAKE_VERSION_PLACEHOLDER, cMakeExtension.version.get())
            )
            if (sourceSets.contains(mainSourceSet)) {
                val fileName = getKotlinSecretsFileName(mainSourceSet).removeSuffix(KOTLIN_FILE_NAME_SUFFIX)
                textBuilder.append(
                    codeGenerator.getCMakeListsCode(
                        sourceSet = mainSourceSet,
                        mappingFileName = fileName,
                        cmakeArgumentName = DEFAULT_CMAKE_ARGUMENT_NAME,
                        isFirstSourceSet = false,
                    )
                )
            }
            sourceSets.groupBy { getCmakeArgumentName(it) }.forEach { map ->
                val (cMakeArgument, sourceSetList) = map
                var isFirstSourceSet = true
                for (sourceSet in sourceSetList) {
                    if (sourceSet == mainSourceSet) {
                        continue
                    }
                    val fileName = getKotlinSecretsFileName(sourceSet).removeSuffix(KOTLIN_FILE_NAME_SUFFIX)
                    textBuilder.append(
                        codeGenerator.getCMakeListsCode(
                            sourceSet = sourceSet,
                            mappingFileName = fileName,
                            cmakeArgumentName = cMakeArgument,
                            isFirstSourceSet,
                        )
                    )
                    isFirstSourceSet = false
                }
                if (sourceSetList.count { sourceSet -> sourceSet != mainSourceSet } > 1) {
                    textBuilder.append("\nendif()\n")
                }
            }
            val destination = getCppDestination(mainSourceSet, fileName = file.name)
            writeTextToFile(destination, textBuilder.toString())
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying CMakeLists file: ${throwable.message}")
            }
        }
    }

    private fun copyKotlinFile(sourceSet: SecretsSourceSet) {
        runCatching {
            val kotlinFileName = if (secretsVaultExtension.makeInjectable.get()) {
                TEMP_KOTLIN_INJECTABLE_FILE_NAME
            } else {
                TEMP_KOTLIN_NOT_INJECTABLE_FILE_NAME
            }
            val kotlinFiles = project.file("${pluginSourceFolder.get().path}/kotlin/").listFiles()
            val kotlinFile = kotlinFiles?.find { file ->
                file.name == kotlinFileName
            } ?: throw IOException("Kotlin file that will be copied not found")
            var text = kotlinFile.readText(Charset.defaultCharset())
            val fileName = getKotlinSecretsFileName(sourceSet)
            text = text.replace(PACKAGE_PLACEHOLDER, packageName)
                .replace(SECRETS_CLASS_NAME_PLACEHOLDER, fileName.removeSuffix(KOTLIN_FILE_NAME_SUFFIX))
            val destination = getKotlinDestination(
                sourceSet = sourceSet,
                fileName = fileName,
            )
            writeTextToFile(destination, text)
        }.onFailure { throwable ->
            if (throwable is IOException) {
                logger.error("Error occurred while copying Kotlin file: ${throwable.message}")
            }
        }
    }

    /**
     * Retrieves the Kotlin secrets file name based on the given source set.
     *
     * For the main source set, a predefined file name will be returned.
     * For other source sets, the function will look up the file name from the source set mapping.
     * If no mapping exists for the given source set, a default file name will be used.
     *
     * The result is then capitalized and appended with a Kotlin file name suffix.
     *
     * @param sourceSet The source set or build flavor for which the secrets file name should be retrieved.
     * @return The Kotlin secrets file name for the provided source set.
     */
    private fun getKotlinSecretsFileName(sourceSet: SecretsSourceSet): String {
        val secretsFilePrefix = if (sourceSet == SecretsSourceSet(MAIN_SOURCE_SET_NAME)) {
            MAIN_SOURCE_SET_SECRETS_FILE_NAME
        } else {
            sourceSetToSecretFileMap?.get(sourceSet)?.first?.name ?: DEFAULT_SECRETS_FILE_NAME
        }
        return secretsFilePrefix.capitalize() + KOTLIN_FILE_NAME_SUFFIX
    }

    /**
     * Retrieves the CMake argument name based on the given source set.
     *
     * If no mapping exists for the given source set, a default argument name will be used.
     *
     * @param sourceSet The source set or build flavor for which the secrets file name should be retrieved.
     * @return The CMake argument name for the provided source set.
     */
    private fun getCmakeArgumentName(sourceSet: SecretsSourceSet): String {
        return sourceSetToSecretFileMap?.get(sourceSet)?.second?.argumentName
            ?: DEFAULT_CMAKE_ARGUMENT_NAME
    }

    /**
     * Keeps the provided secrets for the specified build source set
     *
     * @param sourceSet the source set or build flavor for which the secrets should be hidden
     * @param secrets the list of secrets to be hidden
     */
    private fun keepSecrets(sourceSet: SecretsSourceSet, secrets: List<Secret>) {
        if (secrets.isEmpty()) {
            logWarning("No secrets to hide for the source set $sourceSet.")
            return
        }

        copySecretCppFile(sourceSet)
        copyKotlinFile(sourceSet)

        val fileName = getKotlinSecretsFileName(sourceSet)
        val secretsKotlin = getKotlinDestination(
            sourceSet = sourceSet,
            fileName = fileName,
        )
        val secretKeyToIndexMap = secrets.mapIndexed { index, secret ->
            secret.key to index
        }.toMap()

        // Append Kotlin code
        val kotlinText = secretsKotlin.readText(Charset.defaultCharset()).replace(
            NATIVE_FILE_NAME_PLACEHOLDER,
            fileName.removeSuffix(KOTLIN_FILE_NAME_SUFFIX).lowercase(Locale.ENGLISH)
        ).replace(
            EXTERNAL_METHODS_PLACEHOLDER,
            secrets.joinToString(EMPTY_STRING) {
                codeGenerator.getKotlinCode(
                    keyName = it.key,
                    jvmName = "$JVM_NAME_PREFIX${secretKeyToIndexMap[it.key]}",
                )
            }
        )
        secretsKotlin.writeText(kotlinText, Charset.defaultCharset())

        // Append CPP code
        var kotlinPackage = Utils.getKotlinFilePackage(secretsKotlin)
        if (kotlinPackage.isNullOrEmpty()) {
            logWarning("Empty package in $fileName")
            kotlinPackage = packageName
        }

        val secretsCpp = getCppDestination(sourceSet = sourceSet, fileName = SECRETS_CPP_FILE_NAME)
        secrets.forEach { secret ->
            val (key, value) = secret
            val obfuscatedValue = Utils.encodeSecret(value, secretsVaultExtension.obfuscationKey.get())
            val cppText = secretsCpp.readText(Charset.defaultCharset())
            if (cppText.contains(obfuscatedValue)) {
                logWarning("Key already added in C++ !")
                return@forEach
            }
            secretsCpp.appendText(
                text = codeGenerator.getCppCode(
                    packageName = kotlinPackage,
                    keyName = "$JVM_NAME_PREFIX${secretKeyToIndexMap[key]}",
                    obfuscatedValue = obfuscatedValue,
                    fileName = fileName.removeSuffix(KOTLIN_FILE_NAME_SUFFIX),
                )
            )
        }
        logSuccess(
            "You can now get your secret key for the source set {} by calling : {}().getYourSecretKeyName()",
            sourceSet.sourceSet,
            fileName.removeSuffix(KOTLIN_FILE_NAME_SUFFIX),
        )
    }

    /**
     * Writes the provided text to a file at the specified destination.
     * If the file or its parent directories do not exist, they will be created.
     *
     * @param destination The [File] object representing the file where the text is to be written.
     * @param text The [String] of text to be written to the file.
     * @throws IOException If an I/O error occurred
     */
    private fun writeTextToFile(destination: File, text: String) {
        destination.parentFile?.takeUnless { it.exists() }?.mkdirs()
        destination.takeUnless { it.exists() }?.createNewFile()
        destination.writeText(text)
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
