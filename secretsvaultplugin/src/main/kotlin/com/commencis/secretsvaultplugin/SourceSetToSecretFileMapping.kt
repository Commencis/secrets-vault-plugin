package com.commencis.secretsvaultplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Value class representing a collection of secrets mappings.
 *
 * @property sourceSetToSecretFileMappingArray An array of [SourceSetToSecretFileMapping] objects
 * representing the mapping between source sets and their respective secrets filenames.
 */
@JvmInline
@Serializable
internal value class SourceSetToSecretFileMappingArray(
    val sourceSetToSecretFileMappingArray: Array<SourceSetToSecretFileMapping>
) {

    /**
     * Converts the [SourceSetToSecretFileMappingArray] into a map representation.
     *
     * The resulting map will have [SecretsSourceSet] as keys and their respective
     * secrets file names as values.
     *
     * @return A map with [SecretsSourceSet] as keys and secrets file names as values.
     */
    fun toMap(): Map<SecretsSourceSet, String> {
        return sourceSetToSecretFileMappingArray.flatMap { mapping ->
            mapping.sourceSets.map { sourceSet ->
                sourceSet to mapping.secretsFileName
            }
        }.toMap()
    }
}

/**
 * Value class representing a source set.
 *
 * @property sourceSet A source set name.
 */
@JvmInline
@Serializable
internal value class SecretsSourceSet(val sourceSet: String)

/**
 * Data class representing a mapping between a secrets filename and a list of source sets.
 *
 * @property secretsFileName The name of the Kotlin file containing the secrets.
 * @property sourceSets A list of source set names associated with the specified secrets filename.
 */
@Serializable
internal data class SourceSetToSecretFileMapping(
    @SerialName("secretsFileName")
    val secretsFileName: String,
    @SerialName("sourceSets")
    val sourceSets: List<SecretsSourceSet>,
)
