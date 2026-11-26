package com.miguelaetxio.mia2f.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.gemini.GeminiRepository
import com.miguelaetxio.mia2f.data.gemini.GeminiResult
import com.miguelaetxio.mia2f.data.settings.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the connection test.
 * ---
 * Estado de la prueba de conexión.
 */
sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Running : ConnectionTestState()
    data class Done(val result: GeminiResult) : ConnectionTestState()
}

/**
 * Settings: saving, identifying and deleting the Gemini API key, and
 * testing the connection with a real call.
 * ---
 * Ajustes: guardar, identificar y borrar la clave de la API de Gemini,
 * y probar la conexión con una llamada real.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val geminiRepository: GeminiRepository,
) : ViewModel() {

    private val _testState =
        MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    /**
     * Sends a sentence to Gemini for correction.
     * ---
     * Envía una frase a Gemini para que la corrija.
     */
    fun testConnection(sentence: String) {
        _testState.value = ConnectionTestState.Running
        viewModelScope.launch {
            _testState.value = ConnectionTestState.Done(
                geminiRepository.correctSentence(sentence)
            )
        }
    }

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
