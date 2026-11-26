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
    val generationConfig: GenerationConfig? = null,
)

/**
 * Only the structured output part is used: responseFormat.text with
 * mimeType "APPLICATION_JSON" and a JSON Schema (REST form of the
 * official guide, checked 2026-11-26, see ANNEX_H03).
 * ---
 * Solo se usa la parte de salida estructurada: responseFormat.text
 * con mimeType "APPLICATION_JSON" y un JSON Schema (forma REST de la
 * guía oficial, comprobada el 2026-11-26, ver ANNEX_H03).
 */
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
)

data class ResponseFormat(
    val text: TextResponseFormat? = null,
)

data class TextResponseFormat(
    val mimeType: String,
    val schema: Map<String, Any>? = null,
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
