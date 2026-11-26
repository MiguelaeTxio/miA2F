package com.miguelaetxio.mia2f.data.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the Gemini API key encrypted with an AES-256-GCM key that
 * lives in the Android Keystore and never leaves it. Only the IV and
 * the ciphertext are written to SharedPreferences. This replaces the
 * deprecated EncryptedSharedPreferences, as the official docs advise.
 * ---
 * Guarda la clave de la API de Gemini cifrada con una clave
 * AES-256-GCM que vive en el Android Keystore y nunca sale de él. En
 * SharedPreferences solo se escriben el IV y el texto cifrado. Sustituye
 * a EncryptedSharedPreferences, obsoleto, como indica la documentación
 * oficial.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hasKey = MutableStateFlow(readKey() != null)

    /**
     * Whether a usable key is stored.
     * ---
     * Si hay una clave guardada y utilizable.
     */
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    /**
     * Encrypts and saves the key. Blank input is ignored.
     * ---
     * Cifra y guarda la clave. Ignora una entrada vacía.
     */
    fun saveKey(apiKey: String) {
        val clean = apiKey.trim()
        if (clean.isEmpty()) return
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_IV, encode(cipher.iv))
            .putString(KEY_CIPHER_TEXT, encode(cipherText))
            .apply()
        _hasKey.value = true
    }

    /**
     * Returns the decrypted key, or null if none is stored or it can
     * no longer be decrypted (e.g. the Keystore entry was lost).
     * ---
     * Devuelve la clave descifrada, o null si no hay ninguna o ya no se
     * puede descifrar (por ejemplo, si se perdió la entrada del
     * Keystore).
     */
    fun readKey(): String? {
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val cipherText = prefs.getString(KEY_CIPHER_TEXT, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_BITS, decode(iv)),
            )
            String(cipher.doFinal(decode(cipherText)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Last characters of the stored key, so the user can tell which
     * key is saved without showing it.
     * ---
     * Últimos caracteres de la clave guardada, para que el usuario sepa
     * cuál es sin mostrarla entera.
     */
    fun keySuffix(): String? = readKey()?.takeLast(SUFFIX_LENGTH)

    /**
     * Deletes the stored key.
     * ---
     * Borra la clave guardada.
     */
    fun deleteKey() {
        prefs.edit().remove(KEY_IV).remove(KEY_CIPHER_TEXT).apply()
        _hasKey.value = false
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        (keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .build()
        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray =
        Base64.decode(text, Base64.NO_WRAP)

    private companion object {
        const val PREFS_NAME = "gemini_api_key"
        const val KEY_IV = "iv"
        const val KEY_CIPHER_TEXT = "cipher_text"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "mia2f_gemini_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val AES_KEY_BITS = 256
        const val SUFFIX_LENGTH = 4
    }
}
