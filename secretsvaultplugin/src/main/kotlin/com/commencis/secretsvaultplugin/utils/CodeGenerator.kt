package com.commencis.secretsvaultplugin.utils

/**
 * Helper class that generates various code snippets needed
 */
internal object CodeGenerator {

    /**
     * Placeholder string that is used in the generated C++ code
     * to indicate where the app signature check code should be inserted.
     */
    const val CHECK_APP_SIGNATURE_PLACEHOLDER = "// CHECK_APP_SIGNATURE_PLACEHOLDER"

    /**
     * Generates C++ code for the given package name, key name, obfuscated value, and flavour.
     *
     * @param packageName the package name of the Android app
     * @param keyName the name of the key to be generated
     * @param obfuscatedValue the obfuscated value
     * @param flavour the flavour of the app
     * @return a string containing the generated C++ code
     */
    fun getCppCode(packageName: String, keyName: String, obfuscatedValue: String, flavour: String): String {
        val filePrefix = if (flavour == "main") "Main" else ""
        return """
            |
            |extern "C"
            |JNIEXPORT jstring JNICALL
            |Java_${Utils.getCppName(packageName)}_${filePrefix}Secrets_get${keyName.capitalize()}(
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
     * Generates CMakeLists code for the given flavour and whether it is the first flavour.
     *
     * @param flavour the flavour of the app
     * @param isFirstFlavour whether this is the first flavour
     * @return a string containing the generated CMakeLists code
     */
    fun getCMakeListsCode(flavour: String, isFirstFlavour: Boolean = false): String {
        val elseText = if (!isFirstFlavour) "else" else ""
        return """
            |${elseText}if (FLAVOR STREQUAL "$flavour")
            |    add_library(
            |            secrets
            |            SHARED
            |            ../../$flavour/cpp/secrets.cpp
            |    )
            |
        """.trimMargin()
    }

    /**
     * Generates C++ code for app signature check for a list of app signatures.
     *
     * @param appSignatures the list of app signatures
     * @return a string containing the generated C++ code for app signature check
     */
    fun getAppSignatureCheck(appSignatures: List<String>): String {
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
