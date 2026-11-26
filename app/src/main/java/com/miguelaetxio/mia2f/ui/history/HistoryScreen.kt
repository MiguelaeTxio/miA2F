package com.miguelaetxio.mia2f.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.study.SessionSummary
import com.miguelaetxio.mia2f.ui.common.MarkdownText
import java.text.DateFormat
import java.util.Date

/**
 * List of classes and, when one is opened, its exercises with the
 * answer, the correction and the errors.
 * ---
 * Lista de clases y, al abrir una, sus ejercicios con la respuesta, la
 * corrección y los errores.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val id = openId
    if (id == null) {
        SessionList(viewModel, onBack, onOpen = { openId = it })
    } else {
        SessionDetail(viewModel, id, onBack = { openId = null })
    }
}

@Composable
private fun SessionList(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val sessions by viewModel.sessions.collectAsState()
    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text(
                "Historial de clases",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (sessions.isEmpty()) {
                Text("Todavía no hay ninguna clase.")
            }
        }
        items(sessions, key = { it.id }) { session ->
            SessionCard(viewModel, session, onClick = { onOpen(session.id) })
        }
    }
}

@Composable
private fun SessionCard(
    viewModel: HistoryViewModel,
    session: SessionSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                formatDate(session.startedAt),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                viewModel.unitTitle(session.unitId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${session.answered} ejercicios hechos · " +
                    "${session.errors} errores" +
                    if (session.endedAt == null) " · sin terminar" else "",
            )
        }
    }
}

@Composable
private fun SessionDetail(
    viewModel: HistoryViewModel,
    sessionId: Long,
    onBack: () -> Unit,
) {
    val detailFlow = remember(sessionId) { viewModel.detail(sessionId) }
    val detail by detailFlow.collectAsState(initial = emptyList())
    val session = viewModel.sessions.collectAsState().value
        .firstOrNull { it.id == sessionId }
    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Historial") }
            session?.let {
                Text(
                    viewModel.unitTitle(it.unitId),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    formatDate(it.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(detail, key = { it.first.id }) { (exercise, errors) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Ejercicio ${exercise.position} · " +
                            viewModel.conceptTitle(exercise.conceptId),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    MarkdownText(exercise.prompt)
                    Text(
                        "Tu respuesta: " +
                            (exercise.studentAnswer ?: "sin responder"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    exercise.correction?.let { MarkdownText(it) }
                    errors.forEach {
                        Text(
                            "• ${viewModel.conceptTitle(it.conceptId)}: " +
                                it.description,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        .format(Date(millis))
