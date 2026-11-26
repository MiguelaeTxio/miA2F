package com.miguelaetxio.mia2f.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.course.Concept
import com.miguelaetxio.mia2f.data.course.CourseUnit

/**
 * Read-only syllabus: units, and for the opened one its can-do
 * objectives and concepts.
 * ---
 * Temario de solo lectura: unidades y, de la abierta, sus objetivos y
 * conceptos.
 */
@Composable
fun CourseScreen(
    onBack: () -> Unit,
    viewModel: CourseViewModel = hiltViewModel(),
) {
    val course by viewModel.course.collectAsState()
    var openUnitId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text(
                course?.title ?: "Temario",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                course?.let { "Versión ${it.version} del temario" }
                    ?: "El temario aún no se ha descargado.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(course?.units.orEmpty(), key = { it.id }) { unit ->
            UnitCard(
                unit = unit,
                isOpen = openUnitId == unit.id,
                onToggle = {
                    openUnitId = if (openUnitId == unit.id) null else unit.id
                },
            )
        }
    }
}

@Composable
private fun UnitCard(
    unit: CourseUnit,
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
                "Unidad ${unit.number}. ${unit.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (isOpen) {
                Text("Al terminar…", style = MaterialTheme.typography.labelLarge)
                unit.canDo.forEach {
                    Text("• ${it.text}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                unit.concepts.forEach { ConceptBlock(it) }
            } else {
                Text(
                    "${unit.concepts.size} conceptos",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConceptBlock(concept: Concept) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${typeLabel(concept.type)} · ${concept.title}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(concept.explanation, style = MaterialTheme.typography.bodyMedium)
        concept.examples.forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "grammar" -> "Gramática"
    "vocabulary" -> "Vocabulario"
    "communication" -> "Comunicación"
    "phonetics" -> "Fonética"
    else -> type
}
