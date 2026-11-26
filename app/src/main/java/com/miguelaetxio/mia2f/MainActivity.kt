package com.miguelaetxio.mia2f

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Single entry point of the app. For now it only proves that the
 * skeleton builds, installs and launches.
 * ---
 * Punto de entrada único de la app. De momento solo demuestra que el
 * esqueleto compila, se instala y arranca.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "miA2F — v${BuildConfig.VERSION_NAME}" +
                                " (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}
