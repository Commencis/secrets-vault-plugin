package com.commencis.secretsvaultplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Value class representing a collection of secrets.
 *
 * @property secrets An array of [Secret] objects.
 */
@JvmInline
@Serializable
internal value class Secrets(val secrets: Array<Secret>)

/**
 * Data class representing a Secret
 *
 * @property key The key of the secret
 * @property value The value of the secret
 * @property flavor The flavor of the secret, default is "main"
 */
@Serializable
internal data class Secret(
    @SerialName("key")
    val key: String,
    @SerialName("value")
    val value: String,
    @SerialName("flavor")
    val flavor: String = "main",
)
