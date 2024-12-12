package com.commencis.secretsvaultplugin.sampleapp

internal class FlavorSecretProviderKotlin : FlavorSecretProvider {

    private val secrets = Secrets()

    override val secretPairs: Collection<Pair<String, String>>
        get() = listOf(
            "commonFlavorKey1" to secrets.getCommonFlavorKey1(),
            "prodOnlyKey1" to secrets.getProdOnlyKey1(),
        )

}
