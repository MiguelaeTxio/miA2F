package com.miguelaetxio.mia2f.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.data.gemini.GeminiResult
import com.miguelaetxio.mia2f.ui.common.MarkdownText
import com.miguelaetxio.mia2f.ui.keytransfer.KeyQrDialog
import com.miguelaetxio.mia2f.ui.keytransfer.scanKeyQr
import com.miguelaetxio.mia2f.ui.update.UpdateCheckSection

/**
 * Settings screen: Gemini API key (masked input, stored encrypted)
 * and app updates.
 * ---
 * Pantalla de Ajustes: clave de la API de Gemini (entrada oculta,
 * guardada cifrada) y actualizaciones de la app.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val keySuffix by viewModel.keySuffix.collectAsState()
    // El texto tecleado solo vive en memoria y se borra al guardar.
    // ---
    // Typed text only lives in memory and is cleared on save.
    var typedKey by remember { mutableStateOf("") }
    val testState by viewModel.testState.collectAsState()
    val context = LocalContext.current
    var qrKey by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var testSentence by remember { mutableStateOf(DEFAULT_TEST_SENTENCE) }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Volver") }
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        Text("Clave de la API de Gemini", style = MaterialTheme.typography.titleMedium)
        Text(
            text = keySuffix?.let { "Clave guardada (termina en …$it)." }
                ?: "No hay ninguna clave guardada.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = typedKey,
            onValueChange = { typedKey = it },
            label = {
                Text(if (keySuffix == null) "Pega la clave" else "Sustituir la clave")
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    viewModel.saveKey(typedKey)
                    typedKey = ""
                },
                enabled = typedKey.isNotBlank(),
            ) {
                Text("Guardar")
            }
            if (keySuffix != null) {
                OutlinedButton(onClick = viewModel::deleteKey) {
                    Text("Borrar clave")
                }
            }
        }
        if (keySuffix != null) {
            OutlinedButton(onClick = { qrKey = viewModel.keyForTransfer() }) {
                Text("Pasar la clave a otro móvil")
            }
        } else {
            OutlinedButton(
                onClick = {
                    scanError = null
                    scanKeyQr(
                        context = context,
                        onKey = viewModel::saveKey,
                        onError = { scanError = it },
                    )
                },
            ) {
                Text("Escanear clave desde otro móvil")
            }
        }
        scanError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        qrKey?.let { key ->
            KeyQrDialog(apiKey = key, onDismiss = { qrKey = null })
        }

        HorizontalDivider()

        Text("Probar conexión con Gemini", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = testSentence,
            onValueChange = { testSentence = it },
            label = { Text("Frase en francés") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.testConnection(testSentence) },
            enabled = keySuffix != null &&
                testSentence.isNotBlank() &&
                testState !is ConnectionTestState.Running,
        ) {
            Text("Probar conexión")
        }
        when (val state = testState) {
            is ConnectionTestState.Idle -> Unit
            is ConnectionTestState.Running -> CircularProgressIndicator()
            is ConnectionTestState.Done -> when (val result = state.result) {
                is GeminiResult.Success -> MarkdownText(result.text)
                is GeminiResult.Error -> Text(
                    result.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider()

        Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)
        UpdateCheckSection()
    }
}

// Frase con errores típicos de passé composé para la prueba.
// ---
// Sentence with typical passé composé errors for the test.
private const val DEFAULT_TEST_SENTENCE =
    "Hier, nous sommes allé au cinéma et nous sommes mangé une pizza."
