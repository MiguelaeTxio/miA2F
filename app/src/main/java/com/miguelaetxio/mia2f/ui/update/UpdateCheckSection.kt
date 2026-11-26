package com.miguelaetxio.mia2f.ui.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miguelaetxio.mia2f.BuildConfig

/**
 * "Buscar actualizaciones" section: installed version, check,
 * download with progress and install through the system installer.
 * ---
 * Sección «Buscar actualizaciones»: versión instalada, comprobación,
 * descarga con progreso e instalación con el instalador del sistema.
 */
@Composable
fun UpdateCheckSection(
    viewModel: AppUpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var installErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Versión instalada: ${BuildConfig.VERSION_NAME} " +
                "(${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        when (val state = uiState) {
            is UpdateUiState.Idle -> {
                Button(
                    onClick = {
                        viewModel.checkForUpdate(BuildConfig.VERSION_CODE)
                    },
                ) {
                    Text("Buscar actualizaciones")
                }
            }

            is UpdateUiState.Checking -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Comprobando...")
                }
            }

            is UpdateUiState.UpToDate -> {
                Text("Ya tienes la última versión.")
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = viewModel::dismiss) { Text("Cerrar") }
            }

            is UpdateUiState.UpdateAvailable -> {
                Text(
                    "Hay una versión nueva: " +
                        "${state.manifest.versionName} " +
                        "(${state.manifest.versionCode})."
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.downloadUpdate(state.manifest) },
                    ) {
                        Text("Descargar")
                    }
                    TextButton(onClick = viewModel::dismiss) {
                        Text("Ahora no")
                    }
                }
            }

            is UpdateUiState.Downloading -> {
                val hasTotal = state.totalBytes > 0
                val downloadedMb = state.bytesDownloaded / (1024f * 1024f)
                val totalMb = state.totalBytes / (1024f * 1024f)
                if (hasTotal) {
                    LinearProgressIndicator(
                        progress = {
                            state.bytesDownloaded.toFloat() /
                                state.totalBytes.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hasTotal) {
                        "Descargando... %.1f / %.1f MB (%d%%)".format(
                            downloadedMb,
                            totalMb,
                            (state.bytesDownloaded * 100 / state.totalBytes)
                                .toInt(),
                        )
                    } else {
                        "Descargando... %.1f MB".format(downloadedMb)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is UpdateUiState.ReadyToInstall -> {
                Text("Descarga completa.")
                installErrorMessage?.let { msg ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        installErrorMessage = null
                        // Sin permiso de instalar apps desconocidas, el
                        // Intent de instalación no hace nada en silencio
                        // (MiMoo H07): se manda antes a la pantalla del
                        // sistema donde concederlo.
                        // ---
                        // Without the install-unknown-apps permission,
                        // the install Intent silently does nothing
                        // (MiMoo H07): send the user to the system
                        // screen to grant it first.
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            installErrorMessage = try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                "Concede el permiso y vuelve a pulsar Instalar."
                            } catch (e: Exception) {
                                "Activa «Instalar apps desconocidas» para " +
                                    "miA2F en Ajustes del sistema y vuelve " +
                                    "a intentarlo."
                            }
                            return@Button
                        }
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                state.apkUri,
                                "application/vnd.android.package-archive",
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(installIntent)
                            viewModel.dismiss()
                        } catch (e: Exception) {
                            installErrorMessage =
                                "No se pudo abrir el instalador: ${e.message}"
                        }
                    },
                ) {
                    Text("Instalar")
                }
            }

            is UpdateUiState.Error -> {
                Text(
                    "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = viewModel::dismiss) { Text("Cerrar") }
            }
        }
    }
}
