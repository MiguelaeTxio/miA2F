package com.miguelaetxio.mia2f.ui.profile

import androidx.lifecycle.ViewModel
import com.miguelaetxio.mia2f.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the student profile to the UI.
 * ---
 * Expone el perfil de alumno a la interfaz.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val studentName: StateFlow<String?> = profileRepository.studentName

    /**
     * Saves the name typed on the first-run screen.
     * ---
     * Guarda el nombre escrito en la pantalla de primer arranque.
     */
    fun saveStudentName(name: String) {
        profileRepository.saveStudentName(name)
    }
}
