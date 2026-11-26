package com.miguelaetxio.mia2f.data.gemini

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Gemini Developer API. The key travels in the `x-goog-api-key`
 * header, never in the URL, so it does not end up in any URL log.
 * ---
 * API de Gemini para desarrolladores. La clave viaja en la cabecera
 * `x-goog-api-key`, nunca en la URL, para que no acabe en ningún log
 * de URLs.
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest,
    ): Response<GenerateContentResponse>
}
