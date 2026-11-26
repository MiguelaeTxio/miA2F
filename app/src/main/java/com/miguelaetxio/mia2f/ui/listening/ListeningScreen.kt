package com.miguelaetxio.mia2f.ui.listening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.speech.VoiceStatus
import com.miguelaetxio.mia2f.ui.common.MarkdownText
import com.miguelaetxio.mia2f.ui.common.VoiceNotice

/**
 * Listening practice: hear the dialogue (text hidden), answer the
 * comprehension questions one by one, and see the transcript at the
 * end.
 * ---
 * Práctica de escucha: oír el diálogo (con el texto oculto), contestar
 * las preguntas de comprensión una a una y ver la transcripción al
 * final.
 */
@Composable
fun ListeningScreen(
    onBack: () -> Unit,
    viewModel: ListeningViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.start() }
    val state by viewModel.state.collectAsState()
    val voiceStatus by viewModel.voiceStatus.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        when (val current = state) {
            is ListeningState.Loading -> {
                CircularProgressIndicator()
                Text(current.message)
            }
            is ListeningState.Failed -> {
                Notice(current.message, isError = !current.isOffline)
                Button(onClick = viewModel::retry) { Text("Reintentar") }
            }
            is ListeningState.Working -> {
                Text(
                    "Escucha · " + current.unitTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
                VoiceNotice(
                    status = voiceStatus,
                    unavailableMessage = "Este móvil no tiene voz en " +
                        "francés, así que no puede leer el diálogo.",
                    onRecheck = viewModel::recheckVoice,
                )
                Player(
                    canPlay = voiceStatus == VoiceStatus.READY,
                    isSpeaking = isSpeaking,
                    viewModel = viewModel,
                )
                QuestionContent(current, viewModel)
            }
            is ListeningState.Finished -> FinishedContent(current, onBack)
        }
    }
}

@Composable
private fun Player(
    canPlay: Boolean,
    isSpeaking: Boolean,
    viewModel: ListeningViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Escucha el diálogo tantas veces como quieras. El texto " +
                    "se muestra al terminar las preguntas.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (isSpeaking) {
                OutlinedButton(onClick = viewModel::stopPlaying) {
                    Text("Parar")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.play(slow = false) },
                        enabled = canPlay,
                    ) { Text("Escuchar") }
                    OutlinedButton(
                        onClick = { viewModel.play(slow = true) },
                        enabled = canPlay,
                    ) { Text("Despacio") }
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    state: ListeningState.Working,
    viewModel: ListeningViewModel,
) {
    // La respuesta se vacía al cambiar de pregunta.
    // ---
    // The answer is cleared when the question changes.
    var answer by rememberSaveable(state.number) { mutableStateOf("") }
    Text(
        "Pregunta ${state.number} de ${state.total}",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(state.question, style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        label = { Text("Tu respuesta en castellano") },
        enabled = state.correction == null && !state.isCorrecting,
        modifier = Modifier.fillMaxWidth(),
    )
    val correction = state.correction
    when {
        correction != null -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (correction.isCorrect) "Correcto" else
                            "No del todo",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (correction.isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    MarkdownText(
                        "**Respuesta esperada:** " +
                            "${correction.correctedAnswer}\n\n" +
                            correction.explanation
                    )
                }
            }
            Button(onClick = viewModel::next) {
                Text(
                    if (state.number < state.total) "Siguiente" else
                        "Terminar"
                )
            }
        }
        state.isCorrecting -> {
            CircularProgressIndicator()
            Text("Corrigiendo…")
        }
        else -> {
            Button(
                onClick = { viewModel.submit(answer) },
                enabled = answer.isNotBlank(),
            ) { Text("Corregir") }
            state.message?.let {
                Notice(it, isError = !state.isOffline)
                Button(onClick = viewModel::retry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun FinishedContent(
    state: ListeningState.Finished,
    onBack: () -> Unit,
) {
    Text("Escucha terminada", style = MaterialTheme.typography.headlineSmall)
    Text(state.unitTitle, style = MaterialTheme.typography.titleMedium)
    Text("${state.correct} de ${state.total} respuestas correctas.")
    if (state.errors.isEmpty()) {
        Text("Sin errores en esta práctica.")
    } else {
        Text("Errores:", style = MaterialTheme.typography.titleSmall)
        state.errors.forEach { Text("• $it") }
    }
    Text("El diálogo:", style = MaterialTheme.typography.titleSmall)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.lines.forEach {
                Text(
                    "${ListeningViewModel.speakerName(it.speaker)}: " +
                        it.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
