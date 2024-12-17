package com.commencis.secretsvaultplugin.sampleapp

internal interface FlavorSecretProvider {

    /**
     * Key-value pairs
     */
    val secretPairs: Collection<Pair<String, String>>

}
