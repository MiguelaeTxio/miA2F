package com.miguelaetxio.mia2f.ui.update

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.remote.dto.UpdateManifest
import com.miguelaetxio.mia2f.data.update.AppUpdateRepository
import com.miguelaetxio.mia2f.data.update.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI states of the update section.
 * ---
 * Estados de la sección de actualizaciones.
 */
sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class UpdateAvailable(val manifest: UpdateManifest) :
        UpdateUiState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) :
        UpdateUiState()
    data class ReadyToInstall(val apkUri: Uri) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

/**
 * Drives check → download → install for in-app updates.
 * ---
 * Gobierna comprobar → descargar → instalar en las actualizaciones
 * in-app.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Checks for a newer version; if it is already downloaded, goes
     * straight to install.
     * ---
     * Comprueba si hay versión nueva; si ya está descargada, pasa
     * directamente a instalar.
     */
    fun checkForUpdate(currentVersionCode: Int) {
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            _uiState.value = when (
                val result =
                    appUpdateRepository.checkForUpdate(currentVersionCode)
            ) {
                is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.UpdateAvailable ->
                    appUpdateRepository.downloadedApkUri(result.manifest)
                        ?.let { UpdateUiState.ReadyToInstall(it) }
                        ?: UpdateUiState.UpdateAvailable(result.manifest)
                is UpdateCheckResult.Error ->
                    UpdateUiState.Error(result.message)
            }
        }
    }

    /**
     * Downloads the APK reporting progress.
     * ---
     * Descarga el APK informando del progreso.
     */
    fun downloadUpdate(manifest: UpdateManifest) {
        _uiState.value = UpdateUiState.Downloading(0L, 0L)
        viewModelScope.launch {
            _uiState.value = try {
                val apkUri = appUpdateRepository.downloadApk(manifest) {
                        bytesDownloaded, totalBytes ->
                    _uiState.value =
                        UpdateUiState.Downloading(bytesDownloaded, totalBytes)
                }
                UpdateUiState.ReadyToInstall(apkUri)
            } catch (e: Exception) {
                UpdateUiState.Error(
                    e.message ?: "No se pudo descargar la actualización."
                )
            }
        }
    }

    /**
     * Returns the section to its idle state.
     * ---
     * Devuelve la sección a su estado inicial.
     */
    fun dismiss() {
        _uiState.value = UpdateUiState.Idle
    }
}
