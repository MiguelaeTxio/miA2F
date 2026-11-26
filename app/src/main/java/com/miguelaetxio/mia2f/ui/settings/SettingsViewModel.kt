package com.miguelaetxio.mia2f.ui.settings

import androidx.lifecycle.ViewModel
import com.miguelaetxio.mia2f.data.settings.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Settings: saving, identifying and deleting the Gemini API key.
 * ---
 * Ajustes: guardar, identificar y borrar la clave de la API de Gemini.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    private val _keySuffix = MutableStateFlow(apiKeyStore.keySuffix())

    /**
     * Last characters of the saved key, or null if there is none.
     * ---
     * Últimos caracteres de la clave guardada, o null si no hay.
     */
    val keySuffix: StateFlow<String?> = _keySuffix.asStateFlow()

    /**
     * Saves a new key, replacing any previous one.
     * ---
     * Guarda una clave nueva, sustituyendo la anterior si la hay.
     */
    fun saveKey(apiKey: String) {
        apiKeyStore.saveKey(apiKey)
        _keySuffix.value = apiKeyStore.keySuffix()
    }

    /**
     * Deletes the saved key.
     * ---
     * Borra la clave guardada.
     */
    fun deleteKey() {
        apiKeyStore.deleteKey()
        _keySuffix.value = null
    }
}
