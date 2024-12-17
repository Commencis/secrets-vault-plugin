package com.commencis.secretsvaultplugin.sampleapp

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

internal class MainActivity : ComponentActivity() {

    private val secretPairs: List<Pair<String, String>>
        get() {
            val mainSecrets = MainSecrets()
            val flavorSecretProvider = FlavorSecretProviderKotlin()
            // val flavorSecretProvider = FlavorSecretProviderJava()
            return listOf(
                "generalKey1" to mainSecrets.getGeneralKey1(),
                "generalKey2" to mainSecrets.getGeneralKey2(),
            ) + flavorSecretProvider.secretPairs
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ViewGroup>(android.R.id.content).addTextView()
    }

    private fun ViewGroup.addTextView() {
        val scrollView = ScrollView(context)
        scrollView.setBackgroundColor(Color.WHITE)

        val textView = TextView(scrollView.context)
        textView.setTextColor(Color.BLACK)
        textView.setPadding(64, 64, 64, 64)
        textView.text = secretPairs.joinToString(separator = "\n\n") {
            "${it.first}:\n${it.second}"
        }

        scrollView.addView(
            textView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        )

        addView(
            scrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
    }

}
