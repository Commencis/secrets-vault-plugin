package com.commencis.secretsvaultplugin.sampleapp;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kotlin.Pair;

class FlavorSecretProviderJava implements FlavorSecretProvider {

    private final FlavorSecretProviderJavaHelper secretHelper = new FlavorSecretProviderJavaHelper();

    @NonNull
    @Override
    public Collection<Pair<String, String>> getSecretPairs() {
        final List<Pair<String, String>> list = new ArrayList<>();
        list.add(new Pair<>("commonFlavorKey1", secretHelper.getCommonFlavorKey1()));
        list.add(new Pair<>("devOnlyKey1", secretHelper.getDevOnlyKey1()));
        return list;
    }

}
