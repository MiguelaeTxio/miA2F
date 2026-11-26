package com.miguelaetxio.mia2f.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.ui.common.MarkdownText

/**
 * One class with Gemini: one exercise at a time, its correction, and
 * a summary at the end.
 * ---
 * Una clase con Gemini: un ejercicio cada vez, su corrección y un
 * resumen al final.
 */
@Composable
fun LessonScreen(
    onBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        when (val current = state) {
            is LessonState.Loading -> {
                CircularProgressIndicator()
                Text(current.message)
            }
            is LessonState.Failed -> {
                Notice(current.message, isError = !current.isOffline)
                Button(onClick = viewModel::retry) { Text("Reintentar") }
            }
            is LessonState.Working -> WorkingContent(
                state = current,
                onSubmit = viewModel::submit,
                onNext = viewModel::next,
                onRetry = viewModel::retry,
            )
            is LessonState.Finished -> FinishedContent(current, onBack)
        }
    }
}

@Composable
private fun WorkingContent(
    state: LessonState.Working,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    // La respuesta se vacía al cambiar de ejercicio.
    // ---
    // The answer is cleared when the exercise changes.
    var answer by rememberSaveable(state.number) { mutableStateOf("") }
    Text(state.unitTitle, style = MaterialTheme.typography.titleLarge)
    Text(
        "Ejercicio ${state.number} de ${state.total} · " +
            state.conceptTitle,
        style = MaterialTheme.typography.bodySmall,
    )
    MarkdownText(state.prompt)
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        label = { Text("Tu respuesta en francés") },
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
                            "Con errores",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (correction.isCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    MarkdownText(
                        "**Corrección:** ${correction.correctedAnswer}" +
                            "\n\n${correction.explanation}"
                    )
                }
            }
            Button(onClick = onNext) {
                Text(
                    if (state.number < state.total) "Siguiente" else
                        "Terminar clase"
                )
            }
        }
        state.isCorrecting -> {
            CircularProgressIndicator()
            Text("Corrigiendo…")
        }
        else -> {
            Button(
                onClick = { onSubmit(answer) },
                enabled = answer.isNotBlank(),
            ) {
                Text("Corregir")
            }
            state.message?.let {
                Notice(it, isError = !state.isOffline)
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun FinishedContent(
    state: LessonState.Finished,
    onBack: () -> Unit,
) {
    Text("Clase terminada", style = MaterialTheme.typography.headlineSmall)
    Text(state.unitTitle, style = MaterialTheme.typography.titleMedium)
    Text("${state.correct} de ${state.total} ejercicios correctos.")
    if (state.errors.isEmpty()) {
        Text("Sin errores en esta clase.")
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
