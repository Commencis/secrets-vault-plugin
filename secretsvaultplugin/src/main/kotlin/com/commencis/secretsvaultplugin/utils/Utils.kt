package com.commencis.secretsvaultplugin.utils

import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.experimental.xor

/**
 * Byte mask for hexadecimal conversion
 */
internal const val BYTE_MASK = 0xff

/**
 * Represents the length of the generated obfuscation key.
 */
private const val OBFUSCATION_KEY_LENGTH = 32

/**
 * Utility class for common functionality
 */
internal object Utils {

    /**
     * Transforms names like com.commencis.secrets to com_commencis_secrets to integrate in C++ code.
     * Java package name and key names need to escape some characters to call the NDK.
     * This is needed because certain characters are not allowed or have special meaning in C++ identifiers.
     *
     * @param packageName The package name to be converted to C++ name
     *
     * @return The C++ name
     */
    fun getCppName(packageName: String): String {
        return packageName
            .replace("_", "_1")
            .replace(";", "_2")
            .replace("[", "_3")
            .replace(".", "_")
    }

    /**
     * Encodes a secret by applying a XOR operation with an obfuscator
     *
     * @param secretKey The secret key
     * @param obfuscationKey The key used to generate the obfuscator
     * @return The encoded secret
     */
    fun encodeSecret(secretKey: String, obfuscationKey: String): String {
        // Generate the obfuscator as the SHA-256 hash of the obfuscation key
        val obfuscator = calculateSha256Hash(obfuscationKey)
        val obfuscatorBytes = obfuscator.toByteArray()

        // Generate the obfuscated secret bytes array by applying XOR between the secret and the obfuscator
        val secretBytes = secretKey.toByteArray(Charset.defaultCharset())
        val obfuscatedSecretBytes = secretBytes.mapIndexed { index, secretByte ->
            val obfuscatorByte = obfuscatorBytes[index % obfuscatorBytes.size]
            secretByte.xor(obfuscatorByte)
        }

        // Convert the obfuscated bytes to a hexadecimal string
        val encoded = obfuscatedSecretBytes.joinToString(prefix = "{ ", postfix = " }", separator = ", ") { byte ->
            "0x" + Integer.toHexString(byte.toInt() and BYTE_MASK)
        }
        return encoded
    }

    /**
     * Gets the package of a Kotlin file
     *
     * @param file The Kotlin file
     * @return The package of the file
     */
    fun getKotlinFilePackage(file: File): String? {
        val lines = file.readLines(Charset.defaultCharset())
        val packageLine = lines.firstOrNull { it.startsWith("package ") } ?: return null
        return packageLine.replace("package ", "").replace("`", "")
    }

    /**
     * Generates a random alphanumeric string of defined by [OBFUSCATION_KEY_LENGTH] for obfuscation purposes.
     *
     * This function creates a string of length defined by [OBFUSCATION_KEY_LENGTH] that contains random alphanumeric
     * characters (both lowercase and uppercase letters, and digits).
     *
     * @return A random alphanumeric string of length defined by [OBFUSCATION_KEY_LENGTH].
     */
    fun generateObfuscationKey(): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..OBFUSCATION_KEY_LENGTH)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Returns the SHA-256 hash of a string
     *
     * @param input The string to be hashed
     * @return The SHA-256 hash of the string
     */
    private fun calculateSha256Hash(input: String): String {
        val inputBytes = input.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = messageDigest.digest(inputBytes)

        return hashedBytes.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
