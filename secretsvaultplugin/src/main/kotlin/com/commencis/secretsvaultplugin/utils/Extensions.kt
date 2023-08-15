package com.commencis.secretsvaultplugin.utils

import java.util.Locale

/**
 * Represents an empty string used for default values.
 */
internal const val EMPTY_STRING = ""

/**
 * Capitalizes the first character of the string.
 *
 * @receiver The original string.
 * @return The string with the first character capitalized.
 */
internal fun String.capitalize(): String = replaceFirstChar { firstChar ->
    if (firstChar.isLowerCase()) firstChar.titlecase(Locale.ENGLISH) else firstChar.toString()
}
