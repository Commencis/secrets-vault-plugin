package com.commencis.secretsvaultplugin.utils

import com.commencis.secretsvaultplugin.MAIN_SOURCE_SET_NAME
import com.commencis.secretsvaultplugin.SecretsSourceSet
import java.util.Locale

/**
 * Placeholder string that is used in the generated C++ code
 * to indicate where the app signature check code should be inserted.
 */
internal const val CHECK_APP_SIGNATURE_PLACEHOLDER = "// CHECK_APP_SIGNATURE_PLACEHOLDER"

/**
 * Helper class that generates various code snippets needed
 */
internal class CodeGenerator {
    /**
     * Generates C++ code for the given package name, key name, obfuscated value, and source set.
     *
     * @param packageName the package name of the Android app
     * @param keyName the name of the key to be generated
     * @param obfuscatedValue the obfuscated value
     * @param fileName the name of the generated Kotlin file
     * @return a string containing the generated C++ code
     */
    fun getCppCode(
        packageName: String,
        keyName: String,
        obfuscatedValue: String,
        fileName: String,
    ): String {
        return """
            |
            |extern "C"
            |JNIEXPORT jstring JNICALL
            |Java_${Utils.getCppName(packageName)}_${fileName}_get${keyName.capitalize()}(
            |        JNIEnv* pEnv,
            |        jobject pThis) {
            |     char obfuscatedSecret[] = $obfuscatedValue;
            |     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
            |}
            |
        """.trimMargin()
    }

    /**
     * Generates Kotlin code for the given key name.
     *
     * @param keyName the name of the key to be generated
     * @return a string containing the generated Kotlin code
     */
    fun getKotlinCode(keyName: String): String {
        return """
            |
            |    external fun get${keyName.capitalize()}(): String
            |
        """.trimMargin()
    }

    /**
     * Generates CMakeLists code for the given source set and whether it is the first source set.
     *
     * @param sourceSet the source set of the app
     * @param mappingFileName the file name of the mapped secrets file for the source set
     * @param cmakeArgumentName the name of CMake argument to differentiate between source sets.
     * @param isFirstSourceSet whether this is the first source set
     * @return a string containing the generated CMakeLists code
     */
    fun getCMakeListsCode(
        sourceSet: SecretsSourceSet,
        mappingFileName: String,
        cmakeArgumentName: String,
        isFirstSourceSet: Boolean = false,
    ): String {
        val mainSourceSet = SecretsSourceSet(MAIN_SOURCE_SET_NAME)
        val elseText = EMPTY_STRING.takeIf { isFirstSourceSet } ?: "else"
        val conditionText = EMPTY_STRING.takeIf {
            sourceSet == mainSourceSet
        } ?: "${elseText}if ($cmakeArgumentName STREQUAL \"${sourceSet.sourceSet}\")"
        val sourceSetSecretsPathPrefix = EMPTY_STRING.takeIf {
            sourceSet == mainSourceSet
        } ?: "../../${sourceSet.sourceSet}/cpp/"
        return if (conditionText.isEmpty()) {
            """
            |add_library(
            |        ${mappingFileName.lowercase(Locale.ENGLISH)}
            |        SHARED
            |        secrets.cpp
            |)
            """.trimMargin()
        } else {
            """
            |
            |$conditionText
            |    add_library(
            |            ${mappingFileName.lowercase(Locale.ENGLISH)}
            |            SHARED
            |            ${sourceSetSecretsPathPrefix}secrets.cpp
            |    )
            """.trimMargin()
        }
    }

    /**
     * Generates C++ code for app signature check for a list of app signatures.
     *
     * @param appSignatures the list of app signatures
     * @return a string containing the generated C++ code for app signature check
     */
    fun getAppSignatureCheck(appSignatures: List<String>): String {
        if (appSignatures.isEmpty()) {
            return EMPTY_STRING
        }
        val signatures = appSignatures.joinToString(separator = "") { "        $it,\n" }.trimEnd(',', '\n')
        return """
            |const char appSignatures[][32] = {
            |$signatures
            |    };
            |    if (!checkAppSignatures(appSignatures, ${appSignatures.size}, pEnv)) {
            |        return pEnv->NewStringUTF("");
            |    }
        """.trimMargin()
    }
}
