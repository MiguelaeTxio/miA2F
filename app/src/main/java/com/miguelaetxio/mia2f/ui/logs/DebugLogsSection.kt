package com.miguelaetxio.mia2f.ui.logs

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.backup.DriveAuthorization
import com.miguelaetxio.mia2f.data.backup.DriveAuthorizationHelper
import com.miguelaetxio.mia2f.data.backup.DriveOutcome
import com.miguelaetxio.mia2f.data.backup.ProgressBackupRepository
import com.miguelaetxio.mia2f.data.log.DebugLogCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * "Upload logs to Drive": collects the diagnostic logs and uploads
 * them to the exchange folder, asking for the Google permission if
 * needed.
 * ---
 * «Subir logs a Drive»: reúne los logs de diagnóstico y los sube a la
 * carpeta de intercambio, pidiendo el permiso de Google si hace falta.
 */
@HiltViewModel
class DebugLogsViewModel @Inject constructor(
    private val collector: DebugLogCollector,
    private val repository: ProgressBackupRepository,
    private val authorizationHelper: DriveAuthorizationHelper,
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _consentRequest = MutableStateFlow<IntentSenderRequest?>(null)
    val consentRequest: StateFlow<IntentSenderRequest?> =
        _consentRequest.asStateFlow()

    /**
     * Uploads every log, the manifest last.
     * ---
     * Sube todos los logs, el manifiesto el último.
     */
    fun upload() {
        if (_isBusy.value) return
        _isBusy.value = true
        _message.value = null
        viewModelScope.launch {
            val files = collector.collect()
            _message.value = when (
                val result = repository.uploadDebugLogs(files)
            ) {
                is DriveOutcome.Success -> {
                    val time =
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date())
                    "Logs subidos a Drive a las $time (${result.value} " +
                        "archivos). Ya puedes decir «listo»."
                }
                is DriveOutcome.NeedsConsent -> {
                    _consentRequest.value = result.request
                    null
                }
                DriveOutcome.Offline ->
                    "Sin conexión: no se han podido subir los logs."
                DriveOutcome.ForeignCopy -> null
                is DriveOutcome.Failed -> result.message
            }
            _isBusy.value = false
        }
    }

    /**
     * The permission screen has been launched.
     * ---
     * Se ha lanzado la pantalla de permiso.
     */
    fun onConsentLaunched() {
        _consentRequest.value = null
    }

    /**
     * Result of the permission screen; uploads if granted.
     * ---
     * Resultado de la pantalla de permiso; sube si se concedió.
     */
    fun onConsentResult(data: Intent?) {
        when (val auth = authorizationHelper.fromConsentResult(data)) {
            is DriveAuthorization.Authorized -> upload()
            is DriveAuthorization.Failed -> _message.value = auth.message
            is DriveAuthorization.NeedsUserConsent -> _message.value =
                "Sin el permiso de Google no se pueden subir los logs."
        }
    }
}

/**
 * Settings section with the "Subir logs a Drive" button.
 * ---
 * Sección de Ajustes con el botón «Subir logs a Drive».
 */
@Composable
fun DebugLogsSection(viewModel: DebugLogsViewModel = hiltViewModel()) {
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
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
    Text(
        "Sube los registros de la app a la carpeta «miA2F - Intercambio " +
            "Claude» de tu Drive para que Claude pueda leerlos. No " +
            "incluyen la clave de Gemini.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (isBusy) {
        CircularProgressIndicator()
    } else {
        OutlinedButton(onClick = viewModel::upload) {
            Text("Subir logs a Drive")
        }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
}
