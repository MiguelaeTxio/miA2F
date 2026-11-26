package com.miguelaetxio.mia2f.data.gemini

/**
 * Request and response bodies of `models.generateContent` (Gemini
 * Developer API, REST v1beta). Field names follow the official JSON;
 * null fields are omitted by Gson.
 * ---
 * Cuerpos de petición y respuesta de `models.generateContent` (Gemini
 * Developer API, REST v1beta). Los nombres de campo siguen el JSON
 * oficial; Gson omite los campos nulos.
 */
data class GenerateContentRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
)

data class Content(
    val role: String? = null,
    val parts: List<Part>,
)

data class Part(
    val text: String? = null,
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
)

data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
)

data class GeminiErrorEnvelope(
    val error: GeminiError? = null,
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null,
)
