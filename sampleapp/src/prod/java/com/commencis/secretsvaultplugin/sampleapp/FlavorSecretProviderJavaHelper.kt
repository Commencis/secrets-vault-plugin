package com.commencis.secretsvaultplugin.sampleapp

internal class FlavorSecretProviderJavaHelper {

    private val secrets = Secrets()

    val commonFlavorKey1: String
        get() = secrets.getCommonFlavorKey1()

    val prodOnlyKey1: String
        get() = secrets.getProdOnlyKey1()

}
