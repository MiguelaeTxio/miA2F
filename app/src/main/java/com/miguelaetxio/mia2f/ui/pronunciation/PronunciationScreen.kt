package com.miguelaetxio.mia2f.ui.pronunciation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.speech.FrenchVoice
import com.miguelaetxio.mia2f.data.speech.VoiceRecorder
import com.miguelaetxio.mia2f.data.speech.VoiceStatus
import com.miguelaetxio.mia2f.ui.common.MarkdownText

/**
 * Pronunciation practice: listen to the model sentence, record it and
 * read Gemini's comment on the recording.
 * ---
 * Práctica de pronunciación: escuchar la frase modelo, grabarla y leer
 * el comentario de Gemini sobre la grabación.
 */
@Composable
fun PronunciationScreen(
    onBack: () -> Unit,
    viewModel: PronunciationViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.start() }
    val state by viewModel.state.collectAsState()
    val voiceStatus by viewModel.voiceStatus.collectAsState()
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        VoiceNotice(voiceStatus, onRecheck = viewModel::recheckVoice)
        when (val current = state) {
            is PronunciationState.Loading -> {
                CircularProgressIndicator()
                Text(current.message)
            }
            is PronunciationState.Failed -> {
                Notice(current.message, isError = !current.isOffline)
                Button(onClick = viewModel::retry) { Text("Reintentar") }
            }
            is PronunciationState.Working -> WorkingContent(
                state = current,
                canListen = voiceStatus == VoiceStatus.READY,
                viewModel = viewModel,
            )
            is PronunciationState.Finished ->
                FinishedContent(current, onBack)
        }
    }
}

@Composable
private fun VoiceNotice(status: VoiceStatus, onRecheck: () -> Unit) {
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
                        "poder escuchar las frases; después pulsa " +
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
        VoiceStatus.UNAVAILABLE -> Notice(
            "Este móvil no tiene voz en francés: puedes leer la frase y " +
                "grabarla igualmente.",
            isError = false,
        )
    }
}

@Composable
private fun WorkingContent(
    state: PronunciationState.Working,
    canListen: Boolean,
    viewModel: PronunciationViewModel,
) {
    val context = LocalContext.current
    var hasMic by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var micDenied by rememberSaveable { mutableStateOf(false) }
    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMic = granted
        micDenied = !granted
    }
    Text(
        "Pronunciación · " + state.unitTitle,
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        "Frase ${state.number} de ${state.total} · ${state.conceptTitle}",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        state.sentence,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { viewModel.listen(slow = false) },
            enabled = canListen,
        ) { Text("Escuchar") }
        OutlinedButton(
            onClick = { viewModel.listen(slow = true) },
            enabled = canListen,
        ) { Text("Despacio") }
    }
    val feedback = state.feedback
    when {
        feedback != null -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (feedback.isCorrect) "Bien dicha" else
                            "Hay cosas que mejorar",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (feedback.isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        "Se oye: ${feedback.heard}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MarkdownText(feedback.explanation)
                }
            }
            Button(onClick = viewModel::next) {
                Text(
                    if (state.number < state.total) "Siguiente" else
                        "Terminar"
                )
            }
        }
        state.phase == RecordPhase.SENDING -> {
            CircularProgressIndicator()
            Text("Gemini está escuchando tu grabación…")
        }
        !hasMic -> {
            Text(
                "Para grabarte, miA2F necesita el micrófono. La " +
                    "grabación solo se envía a Gemini para comentarla y " +
                    "se borra en cuanto responde.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (micDenied) {
                Notice(
                    "Sin el permiso de micrófono no se puede grabar. " +
                        "Si no vuelve a salir la pregunta, actívalo en " +
                        "los ajustes del móvil, en la app miA2F.",
                    isError = true,
                )
            }
            Button(
                onClick = { askMic.launch(Manifest.permission.RECORD_AUDIO) },
            ) { Text("Permitir el micrófono") }
        }
        state.phase == RecordPhase.RECORDING -> {
            Text(
                "Grabando… di la frase y pulsa «Parar» (como mucho " +
                    "${VoiceRecorder.MAX_DURATION_MS / 1000} segundos).",
            )
            Button(
                onClick = viewModel::stopRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Parar") }
        }
        else -> {
            Button(onClick = viewModel::startRecording) { Text("Grabar") }
            if (state.canResend) {
                OutlinedButton(onClick = viewModel::resend) {
                    Text("Reenviar")
                }
            }
        }
    }
    state.message?.let { Notice(it, isError = true) }
}

@Composable
private fun FinishedContent(
    state: PronunciationState.Finished,
    onBack: () -> Unit,
) {
    Text(
        "Práctica terminada",
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(state.unitTitle, style = MaterialTheme.typography.titleMedium)
    Text("${state.correct} de ${state.total} frases bien dichas.")
    if (state.errors.isEmpty()) {
        Text("Sin errores en esta práctica.")
    } else {
        Text("Errores:", style = MaterialTheme.typography.titleSmall)
        state.errors.forEach { Text("• $it") }
    }
    Button(onClick = onBack) { Text("Volver al inicio") }
}

@Composable
private fun Notice(message: String, isError: Boolean) {
    if (isError) {
        Text(message, color = MaterialTheme.colorScheme.error)
    } else {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
