package com.commencis.secretsvaultplugin.sampleapp

class MainSecrets {

    @JvmName("a0")
    external fun getGeneralKey1(): String

    @JvmName("a1")
    external fun getGeneralKey2(): String

    companion object {
        init {
            System.loadLibrary("mainsecrets")
        }
    }
}
