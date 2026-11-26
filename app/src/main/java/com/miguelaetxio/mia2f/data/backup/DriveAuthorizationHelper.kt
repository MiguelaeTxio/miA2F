package com.miguelaetxio.mia2f.data.backup

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.LogArea
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Result of asking for Drive access. Authorized is the usual case
 * once the student has accepted; NeedsUserConsent means the Google
 * permission screen must be shown from an Activity.
 * ---
 * Resultado de pedir acceso a Drive. Authorized es lo normal una vez
 * que el alumno ha aceptado; NeedsUserConsent significa que hay que
 * mostrar la pantalla de permiso de Google desde una Activity.
 */
sealed class DriveAuthorization {
    data class Authorized(val accessToken: String) : DriveAuthorization()

    data class NeedsUserConsent(
        val request: IntentSenderRequest,
    ) : DriveAuthorization()

    data class Failed(val message: String) : DriveAuthorization()
}

/**
 * Drive authorization with Identity.getAuthorizationClient and the
 * drive.file scope (only the files the app creates), as in MiMoo H06.
 * There is no server, so no offline access: authorize() is called
 * whenever a token is needed and Google returns it without user
 * interaction while the permission stands.
 * ---
 * Autorización de Drive con Identity.getAuthorizationClient y el scope
 * drive.file (solo los archivos que crea la app), como en MiMoo H06.
 * No hay servidor, así que no hay acceso sin conexión: se llama a
 * authorize() cada vez que hace falta un token y Google lo devuelve
 * sin interacción mientras el permiso siga concedido.
 */
@Singleton
class DriveAuthorizationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val client get() = Identity.getAuthorizationClient(context)

    /**
     * Asks for (or silently renews) the Drive token.
     * ---
     * Pide (o renueva sin preguntar) el token de Drive.
     */
    suspend fun authorize(): DriveAuthorization =
        suspendCancellableCoroutine { continuation ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            client.authorize(request)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(toAuthorization(result))
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(failure(e))
                    }
                }
        }

    /**
     * Reads the result of the permission screen. As in Google's
     * example (and MiMoo S006), the result code is not checked first:
     * getAuthorizationResultFromIntent says whether access was given.
     * ---
     * Lee el resultado de la pantalla de permiso. Como en el ejemplo de
     * Google (y MiMoo S006), no se mira antes el código de resultado:
     * getAuthorizationResultFromIntent dice si se concedió el acceso.
     */
    fun fromConsentResult(data: Intent?): DriveAuthorization =
        try {
            toAuthorization(client.getAuthorizationResultFromIntent(data))
        } catch (e: ApiException) {
            failure(e)
        }

    private fun toAuthorization(
        result: AuthorizationResult,
    ): DriveAuthorization {
        val pendingIntent = result.pendingIntent
        val token = result.accessToken
        return when {
            result.hasResolution() && pendingIntent != null -> {
                AppLog.log(LogArea.DRIVE, "Drive pide el permiso al alumno")
                DriveAuthorization.NeedsUserConsent(
                    IntentSenderRequest.Builder(pendingIntent.intentSender)
                        .build()
                )
            }
            token != null -> DriveAuthorization.Authorized(token)
            else -> DriveAuthorization.Failed(
                "Google no ha devuelto acceso a Drive."
            )
        }
    }

    private fun failure(e: Exception): DriveAuthorization.Failed {
        AppLog.error(LogArea.DRIVE, "Autorización de Drive fallida", e)
        return DriveAuthorization.Failed(
            if (e is ApiException) {
                "Google no ha dado acceso a Drive (código ${e.statusCode})."
            } else {
                "No se pudo pedir acceso a Drive: ${e.message}"
            }
        )
    }

    private companion object {
        const val DRIVE_FILE_SCOPE =
            "https://www.googleapis.com/auth/drive.file"
    }
}
