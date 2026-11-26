package com.miguelaetxio.mia2f.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.study.ConceptStatus

/**
 * "Mi progreso": per unit, how many concepts are in each state and,
 * for the opened unit, every concept with its state; concepts under
 * watch show their recent errors.
 * ---
 * «Mi progreso»: por unidad, cuántos conceptos hay en cada estado y,
 * de la unidad abierta, cada concepto con su estado; los que están en
 * observación muestran sus errores recientes.
 */
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val units by viewModel.units.collectAsState()
    var openUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text(
                "Mi progreso",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Un concepto con errores pasa a superado cuando lo " +
                    "aciertas sin errores en dos clases distintas.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (units == null) CircularProgressIndicator()
        }
        items(units.orEmpty(), key = { it.unit.id }) { unitProgress ->
            UnitCard(
                unitProgress = unitProgress,
                isOpen = openUnitId == unitProgress.unit.id,
                onToggle = {
                    openUnitId = if (openUnitId == unitProgress.unit.id) {
                        null
                    } else {
                        unitProgress.unit.id
                    }
                },
            )
        }
    }
}

@Composable
private fun UnitCard(
    unitProgress: UnitProgress,
    isOpen: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Unidad ${unitProgress.unit.number}. " +
                    unitProgress.unit.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                ConceptStatus.entries.joinToString(" · ") {
                    "${unitProgress.count(it)} ${countLabel(it)}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (isOpen) {
                HorizontalDivider()
                unitProgress.rows.forEach { ConceptLine(it) }
            }
        }
    }
}

@Composable
private fun ConceptLine(row: ConceptRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(row.concept.title, style = MaterialTheme.typography.titleSmall)
        Text(
            statusLabel(row.progress.status),
            style = MaterialTheme.typography.labelMedium,
            color = statusColor(row.progress.status),
        )
        if (row.progress.status == ConceptStatus.WATCHED) {
            row.progress.recentErrors.forEach {
                Text(
                    "• $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun countLabel(status: ConceptStatus): String = when (status) {
    ConceptStatus.NOT_WORKED -> "sin trabajar"
    ConceptStatus.CORRECT -> "acertados"
    ConceptStatus.WATCHED -> "en observación"
    ConceptStatus.PASSED -> "superados"
}

private fun statusLabel(status: ConceptStatus): String = when (status) {
    ConceptStatus.NOT_WORKED -> "Sin trabajar"
    ConceptStatus.CORRECT -> "Acertado"
    ConceptStatus.WATCHED -> "En observación"
    ConceptStatus.PASSED -> "Superado"
}

@Composable
private fun statusColor(status: ConceptStatus): Color = when (status) {
    ConceptStatus.NOT_WORKED -> MaterialTheme.colorScheme.onSurfaceVariant
    ConceptStatus.CORRECT -> MaterialTheme.colorScheme.secondary
    ConceptStatus.WATCHED -> MaterialTheme.colorScheme.error
    ConceptStatus.PASSED -> MaterialTheme.colorScheme.primary
}
