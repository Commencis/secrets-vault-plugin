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
     * The resulting map will have [SecretsSourceSet] as keys. Each key will be associated with
     * a pair containing the secrets file name and its corresponding CMake argument.
     *
     * @return A map with [SecretsSourceSet] as keys and pairs of [SecretsFileName] and [CMakeArgument] as values.
     */
    fun toMap(): Map<SecretsSourceSet, Pair<SecretsFileName, CMakeArgument>> {
        return sourceSetToSecretFileMappingArray.flatMap { mapping ->
            mapping.sourceSets.map { sourceSet ->
                sourceSet to Pair(
                    first = mapping.secretsFileName,
                    second = mapping.cmakeArgument,
                )
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
 * @property cmakeArgument The name of CMake argument to differentiate between source sets.
 * @property sourceSets A list of source set names associated with the specified secrets filename.
 */
@Serializable
internal data class SourceSetToSecretFileMapping(
    @SerialName("secretsFileName")
    val secretsFileName: SecretsFileName,
    @SerialName("cmakeArgument")
    val cmakeArgument: CMakeArgument,
    @SerialName("sourceSets")
    val sourceSets: List<SecretsSourceSet>,
)

/**
 * Value class representing a CMake argument name.
 *
 * @property argumentName The name of CMake argument to differentiate between source sets.
 */
@JvmInline
@Serializable
internal value class CMakeArgument(val argumentName: String)

/**
 * Value class representing a secrets file name.
 *
 * @property name The name of the Kotlin file containing the secrets.
 */
@JvmInline
@Serializable
internal value class SecretsFileName(val name: String)
