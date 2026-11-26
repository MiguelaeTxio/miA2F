package com.miguelaetxio.mia2f.ui.backup

import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.backup.DriveAuthorization
import com.miguelaetxio.mia2f.data.backup.DriveAuthorizationHelper
import com.miguelaetxio.mia2f.data.backup.DriveCopy
import com.miguelaetxio.mia2f.data.backup.DriveOutcome
import com.miguelaetxio.mia2f.data.backup.ProgressBackupRepository
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Copy to Drive from the UI: automatic retry while something is
 * pending, "Copy now", and the Google permission screen when needed.
 * ---
 * Copia en Drive desde la interfaz: reintento automático mientras
 * haya algo pendiente, «Copiar ahora» y la pantalla de permiso de
 * Google cuando hace falta.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: ProgressBackupRepository,
    private val authorizationHelper: DriveAuthorizationHelper,
    studySessionDao: StudySessionDao,
) : ViewModel() {

    private enum class Action { COPY, FIND }

    val lastBackupAt: StateFlow<Long?> = repository.lastBackupAt
    val isPending: StateFlow<Boolean> = repository.isPending

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _consentRequest = MutableStateFlow<IntentSenderRequest?>(null)

    /**
     * Google permission screen to launch, or null.
     * ---
     * Pantalla de permiso de Google que hay que lanzar, o null.
     */
    val consentRequest: StateFlow<IntentSenderRequest?> =
        _consentRequest.asStateFlow()

    private val _hasForeignCopy = MutableStateFlow(false)

    /**
     * True when Drive holds a copy from another phone and the student
     * must choose between restoring it and replacing it.
     * ---
     * True cuando Drive tiene una copia de otro móvil y el alumno debe
     * elegir entre restaurarla y sustituirla.
     */
    val hasForeignCopy: StateFlow<Boolean> = _hasForeignCopy.asStateFlow()

    /**
     * True on a phone with no classes yet that has not answered the
     * "already used miA2F on another phone?" offer.
     * ---
     * True en un móvil sin clases todavía que no ha contestado la
     * oferta «¿ya usabas miA2F en otro móvil?».
     */
    val showRestoreOffer: StateFlow<Boolean> = combine(
        studySessionDao.observeAll(),
        repository.isRestoreOfferDismissed,
    ) { sessions, dismissed -> sessions.isEmpty() && !dismissed }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _foundCopy = MutableStateFlow<DriveCopy?>(null)

    /**
     * Copy found in Drive, waiting for the student to confirm the
     * restore.
     * ---
     * Copia encontrada en Drive, a la espera de que el alumno confirme
     * la restauración.
     */
    val foundCopy: StateFlow<DriveCopy?> = _foundCopy.asStateFlow()

    // Tras rechazar el permiso no se vuelve a lanzar solo.
    // ---
    // After the permission is declined it is not launched again alone.
    private var consentDeclined = false
    private var lastForce = false
    private var lastAction = Action.COPY

    /**
     * Copies if something is pending; called when the home screen
     * shows up. Never asks again for a permission already declined.
     * ---
     * Copia si hay algo pendiente; se llama al mostrarse el inicio.
     * Nunca vuelve a pedir un permiso ya rechazado.
     */
    fun copyIfPending() {
        if (repository.isPending.value && !consentDeclined) copy(false)
    }

    /**
     * "Copy now"; force = true replaces a copy from another phone.
     * ---
     * «Copiar ahora»; force = true sustituye una copia de otro móvil.
     */
    fun copyNow(force: Boolean = false) {
        consentDeclined = false
        copy(force)
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
     * Result of the permission screen; continues the copy if granted.
     * ---
     * Resultado de la pantalla de permiso; sigue con la copia si se
     * concedió.
     */
    fun onConsentResult(data: Intent?) {
        when (val auth = authorizationHelper.fromConsentResult(data)) {
            is DriveAuthorization.Authorized -> when (lastAction) {
                Action.COPY -> copy(lastForce)
                Action.FIND -> findCopy()
            }
            is DriveAuthorization.NeedsUserConsent,
            is DriveAuthorization.Failed -> {
                consentDeclined = true
                _message.value = (auth as? DriveAuthorization.Failed)
                    ?.message ?: "Sin el permiso de Google no se puede " +
                    "copiar el progreso en Drive."
            }
        }
    }

    /**
     * The student says this phone starts from scratch.
     * ---
     * El alumno dice que este móvil empieza de cero.
     */
    fun dismissRestoreOffer() {
        repository.dismissRestoreOffer()
    }

    /**
     * Looks for the copy in the Drive of the chosen account; if there
     * is one, it waits in foundCopy for confirmation.
     * ---
     * Busca la copia en el Drive de la cuenta elegida; si la hay,
     * espera en foundCopy a que se confirme.
     */
    fun findCopy() {
        if (_isBusy.value) return
        lastAction = Action.FIND
        consentDeclined = false
        _isBusy.value = true
        viewModelScope.launch {
            _message.value = when (val result = repository.download()) {
                is DriveOutcome.Success -> {
                    _foundCopy.value = result.value
                    if (result.value == null) {
                        "No hay ninguna copia de miA2F en el Drive de " +
                            "esta cuenta."
                    } else {
                        null
                    }
                }
                is DriveOutcome.NeedsConsent -> {
                    _consentRequest.value = result.request
                    null
                }
                DriveOutcome.Offline ->
                    "Sin conexión: hace falta Internet para buscar la copia."
                DriveOutcome.ForeignCopy -> null
                is DriveOutcome.Failed -> result.message
            }
            _isBusy.value = false
        }
    }

    /**
     * Replaces the local progress with the copy found.
     * ---
     * Sustituye el progreso local por la copia encontrada.
     */
    fun confirmRestore() {
        val copy = _foundCopy.value ?: return
        _foundCopy.value = null
        _isBusy.value = true
        viewModelScope.launch {
            repository.restore(copy)
            _hasForeignCopy.value = false
            val classes = copy.backup.sessions.size
            _message.value = "Progreso restaurado: " +
                if (classes == 1) "1 clase." else "$classes clases."
            _isBusy.value = false
        }
    }

    /**
     * The student does not restore the copy found.
     * ---
     * El alumno no restaura la copia encontrada.
     */
    fun cancelRestore() {
        _foundCopy.value = null
    }

    private fun copy(force: Boolean) {
        if (_isBusy.value) return
        lastAction = Action.COPY
        lastForce = force
        _isBusy.value = true
        viewModelScope.launch {
            _message.value = when (val result = repository.upload(force)) {
                is DriveOutcome.Success -> {
                    _hasForeignCopy.value = false
                    "Copia guardada en Drive."
                }
                is DriveOutcome.NeedsConsent -> {
                    _consentRequest.value = result.request
                    null
                }
                DriveOutcome.Offline ->
                    "Sin conexión: la copia se hará cuando vuelva Internet."
                DriveOutcome.ForeignCopy -> {
                    _hasForeignCopy.value = true
                    "Tu Drive ya tiene una copia hecha desde otro móvil. " +
                        "En Ajustes puedes restaurarla o sustituirla por " +
                        "el progreso de este móvil."
                }
                is DriveOutcome.Failed -> result.message
            }
            _isBusy.value = false
        }
    }
}
