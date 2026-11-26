package com.miguelaetxio.mia2f.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * First-run screen: asks for the student's name.
 * ---
 * Pantalla de primer arranque: pide el nombre del alumno.
 */
@Composable
fun ProfileSetupScreen(
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Bienvenido a miA2F",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Este dispositivo es de un solo alumno. ¿Cómo te llamas?",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(name) },
            enabled = name.isNotBlank(),
        ) {
            Text("Guardar")
        }
    }
}
