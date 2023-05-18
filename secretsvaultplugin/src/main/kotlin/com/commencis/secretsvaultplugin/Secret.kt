package com.commencis.secretsvaultplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a Secret
 *
 * @property key The key of the secret
 * @property value The value of the secret
 * @property flavour The flavor of the secret, default is "main"
 */
@Serializable
internal data class Secret(
    @SerialName("key")
    val key: String,
    @SerialName("value")
    val value: String,
    @SerialName("flavour")
    val flavour: String = "main",
)
