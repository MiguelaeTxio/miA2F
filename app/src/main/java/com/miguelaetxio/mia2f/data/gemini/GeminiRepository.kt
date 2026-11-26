package com.miguelaetxio.mia2f.data.gemini

import com.google.gson.Gson
import com.miguelaetxio.mia2f.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point to Gemini. The model ID lives only here.
 * `gemini-3.5-flash` is the model CampuStudiOnline already uses in
 * production and appears on the official pricing page (verified
 * 2026-11-26); the final choice is revisited in H02.
 * ---
 * Punto único de acceso a Gemini. El identificador del modelo vive
 * solo aquí. `gemini-3.5-flash` es el modelo que CampuStudiOnline ya
 * usa en producción y figura en la página oficial de precios
 * (comprobado el 2026-11-26); la elección definitiva se revisa en H02.
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val apiKeyStore: ApiKeyStore,
) {

    /**
     * Sends a French sentence and returns Gemini's correction in
     * Spanish, following the FrancaisA2 method. Never throws.
     * ---
     * Envía una frase en francés y devuelve la corrección de Gemini en
     * castellano, con el método de FrancaisA2. Nunca lanza excepciones.
     */
    suspend fun correctSentence(sentence: String): GeminiResult =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyStore.readKey()
                ?: return@withContext GeminiResult.Error(
                    "No hay ninguna clave de Gemini guardada."
                )
            val request = GenerateContentRequest(
                systemInstruction = Content(
                    parts = listOf(Part(text = CORRECTION_INSTRUCTION)),
                ),
                contents = listOf(
                    Content(role = "user", parts = listOf(Part(text = sentence))),
                ),
            )
            try {
                val response =
                    geminiApiService.generateContent(MODEL_ID, apiKey, request)
                if (!response.isSuccessful) {
                    val apiMessage = response.errorBody()?.string()?.let {
                        runCatching {
                            Gson().fromJson(it, GeminiErrorEnvelope::class.java)
                                ?.error?.message
                        }.getOrNull()
                    }
                    return@withContext GeminiResult.Error(
                        "Error ${response.code()}: " +
                            (apiMessage ?: "respuesta no válida de Gemini.")
                    )
                }
                val text = response.body()?.candidates
                    ?.firstOrNull()?.content?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString("")
                    ?.trim()
                if (text.isNullOrEmpty()) {
                    GeminiResult.Error("Gemini no ha devuelto texto.")
                } else {
                    GeminiResult.Success(text)
                }
            } catch (e: Exception) {
                GeminiResult.Error(
                    "No se pudo conectar con Gemini: ${e.message}"
                )
            }
        }

    private companion object {
        const val MODEL_ID = "gemini-3.5-flash"

        // Método de corrección de FrancaisA2 (MASTER_DOCUMENT §4.8).
        // ---
        // FrancaisA2 correction method (MASTER_DOCUMENT §4.8).
        const val CORRECTION_INSTRUCTION =
            "Eres profesor de francés de un alumno hispanohablante de " +
                "nivel A2. Corrige la frase en francés que te envía. " +
                "Responde en castellano: primero la frase corregida, " +
                "después cada error con una explicación breve. Sé " +
                "honesto y directo: no silencies errores ni inventes " +
                "reglas. Si la frase es correcta, dilo."
    }
}

/**
 * Result of a Gemini call.
 * ---
 * Resultado de una llamada a Gemini.
 */
sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}
