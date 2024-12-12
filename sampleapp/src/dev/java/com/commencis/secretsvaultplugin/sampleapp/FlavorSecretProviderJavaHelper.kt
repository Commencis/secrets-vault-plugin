package com.commencis.secretsvaultplugin.sampleapp

internal class FlavorSecretProviderJavaHelper {

    private val secrets = Secrets()

    val commonFlavorKey1: String
        get() = secrets.getCommonFlavorKey1()

    val devOnlyKey1: String
        get() = secrets.getDevOnlyKey1()

}
