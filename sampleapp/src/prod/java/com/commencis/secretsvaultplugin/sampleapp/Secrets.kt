package com.commencis.secretsvaultplugin.sampleapp

class Secrets {

    @JvmName("a0")
    external fun getCommonFlavorKey1(): String

    @JvmName("a1")
    external fun getProdOnlyKey1(): String

    companion object {
        init {
            System.loadLibrary("secrets")
        }
    }
}
