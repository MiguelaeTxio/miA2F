package com.miguelaetxio.mia2f.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

/**
 * Launches the Google permission screen whenever the ViewModel asks
 * for it and hands the result back.
 * ---
 * Lanza la pantalla de permiso de Google cuando el ViewModel la pide
 * y le devuelve el resultado.
 */
@Composable
private fun ConsentLauncher(viewModel: BackupViewModel) {
    val request by viewModel.consentRequest.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { viewModel.onConsentResult(it.data) }
    LaunchedEffect(request) {
        request?.let {
            viewModel.onConsentLaunched()
            launcher.launch(it)
        }
    }
}

/**
 * Home screen: retries a pending copy every time it shows up, and
 * only takes space while a copy is pending.
 * ---
 * Inicio: reintenta la copia pendiente cada vez que se muestra, y
 * solo ocupa sitio mientras haya una copia pendiente.
 */
@Composable
fun PendingBackupNotice(viewModel: BackupViewModel) {
    ConsentLauncher(viewModel)
    LaunchedEffect(Unit) { viewModel.copyIfPending() }
    val isPending by viewModel.isPending.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    if (!isPending) return
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Hay progreso sin copiar en Drive.",
                style = MaterialTheme.typography.bodyMedium,
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (isBusy) {
                CircularProgressIndicator()
            } else {
                OutlinedButton(onClick = { viewModel.copyNow() }) {
                    Text("Copiar ahora")
                }
            }
        }
    }
}

/**
 * Settings: last copy, "Copy now", "Restore from Drive" and, if Drive
 * holds a copy from another phone, the option to replace it.
 * ---
 * Ajustes: última copia, «Copiar ahora», «Restaurar desde Drive» y, si
 * Drive tiene una copia de otro móvil, la opción de sustituirla.
 */
@Composable
fun DriveBackupSection(viewModel: BackupViewModel) {
    ConsentLauncher(viewModel)
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val hasForeignCopy by viewModel.hasForeignCopy.collectAsState()
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    Text(
        lastBackupAt?.let {
            "Última copia: " + DateFormat
                .getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
                .format(Date(it))
        } ?: "Todavía no hay ninguna copia de este móvil en Drive.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        "La copia se guarda en el Drive de la cuenta de Google que elijas " +
            "y se actualiza sola al terminar cada clase. No incluye la " +
            "clave de Gemini.",
        style = MaterialTheme.typography.bodySmall,
    )
    RestoreDialog(viewModel)
    if (isBusy) {
        CircularProgressIndicator()
    } else {
        Button(onClick = { viewModel.copyNow() }) { Text("Copiar ahora") }
        OutlinedButton(onClick = viewModel::findCopy) {
            Text("Restaurar desde Drive")
        }
        if (hasForeignCopy) {
            OutlinedButton(onClick = { confirmReplace = true }) {
                Text("Sustituir la copia de Drive por la de este móvil")
            }
        }
    }
    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("¿Sustituir la copia de Drive?") },
            text = {
                Text(
                    "La copia que hay en tu Drive se borrará y se guardará " +
                        "en su lugar el progreso de este móvil. Si esa " +
                        "copia tiene clases que este móvil no tiene, se " +
                        "perderán."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplace = false
                        viewModel.copyNow(force = true)
                    },
                ) { Text("Sustituir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
}

/**
 * Confirmation before restoring: date of the copy and how many classes
 * it brings, since it replaces the progress of this phone.
 * ---
 * Confirmación antes de restaurar: fecha de la copia y cuántas clases
 * trae, porque sustituye el progreso de este móvil.
 */
@Composable
private fun RestoreDialog(viewModel: BackupViewModel) {
    val copy by viewModel.foundCopy.collectAsState()
    val backup = copy?.backup ?: return
    val classes = backup.sessions.size
    AlertDialog(
        onDismissRequest = viewModel::cancelRestore,
        title = { Text("¿Restaurar la copia de Drive?") },
        text = {
            Text(
                "Copia del " + DateFormat
                    .getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
                    .format(Date(backup.exportedAt)) +
                    (backup.studentName?.let { ", de $it" } ?: "") +
                    ", con " +
                    (if (classes == 1) "1 clase" else "$classes clases") +
                    ". Sustituye todo el progreso de este móvil."
            )
        },
        confirmButton = {
            TextButton(onClick = viewModel::confirmRestore) {
                Text("Restaurar")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelRestore) {
                Text("Cancelar")
            }
        },
    )
}

/**
 * First run: offers to bring back the progress from another phone
 * while this one has no classes yet.
 * ---
 * Primer arranque: ofrece recuperar el progreso de otro móvil
 * mientras este no tenga ninguna clase.
 */
@Composable
fun RestoreOffer(viewModel: BackupViewModel) {
    val show by viewModel.showRestoreOffer.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    RestoreDialog(viewModel)
    if (!show) return
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "¿Ya usabas miA2F en otro móvil? Puedes recuperar tu " +
                    "progreso desde tu Drive.",
                style = MaterialTheme.typography.bodyMedium,
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (isBusy) {
                CircularProgressIndicator()
            } else {
                Button(onClick = viewModel::findCopy) {
                    Text("Recuperar mi progreso")
                }
                OutlinedButton(onClick = viewModel::dismissRestoreOffer) {
                    Text("No, empiezo de cero")
                }
            }
        }
    }
}
