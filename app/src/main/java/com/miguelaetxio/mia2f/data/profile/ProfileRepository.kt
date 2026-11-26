package com.miguelaetxio.mia2f.data.profile

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single student profile of this installation (one device, one
 * student). The name is not sensitive, so plain SharedPreferences.
 * ---
 * El único perfil de alumno de esta instalación (un dispositivo, un
 * alumno). El nombre no es sensible: SharedPreferences normales.
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _studentName =
        MutableStateFlow(prefs.getString(KEY_STUDENT_NAME, null))

    /**
     * Current student name, or null before the first-run setup.
     * ---
     * Nombre del alumno, o null antes de la configuración inicial.
     */
    val studentName: StateFlow<String?> = _studentName.asStateFlow()

    /**
     * Saves the trimmed name. Blank names are ignored.
     * ---
     * Guarda el nombre sin espacios sobrantes. Ignora nombres vacíos.
     */
    fun saveStudentName(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        prefs.edit().putString(KEY_STUDENT_NAME, clean).apply()
        _studentName.value = clean
    }

    private companion object {
        const val PREFS_NAME = "student_profile"
        const val KEY_STUDENT_NAME = "student_name"
    }
}
