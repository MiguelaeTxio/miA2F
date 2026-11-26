package com.miguelaetxio.mia2f.ui.common

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.miguelaetxio.mia2f.data.speech.FrenchVoice
import com.miguelaetxio.mia2f.data.speech.VoiceStatus

/**
 * Warns when the phone's French voice is missing (with a button to
 * install it) or unavailable.
 * ---
 * Avisa cuando falta la voz francesa del móvil (con botón para
 * instalarla) o no está disponible.
 */
@Composable
fun VoiceNotice(
    status: VoiceStatus,
    unavailableMessage: String,
    onRecheck: () -> Unit,
) {
    val context = LocalContext.current
    when (status) {
        VoiceStatus.LOADING, VoiceStatus.READY -> Unit
        VoiceStatus.MISSING_DATA -> Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Falta la voz francesa en este móvil. Instálala para " +
                        "poder escucharla; después pulsa " +
                        "«Comprobar de nuevo».",
                )
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(FrenchVoice.ACTION_INSTALL_VOICE)
                            )
                        }
                    },
                ) { Text("Instalar la voz francesa") }
                OutlinedButton(onClick = onRecheck) {
                    Text("Comprobar de nuevo")
                }
            }
        }
        VoiceStatus.UNAVAILABLE -> Text(
            unavailableMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
