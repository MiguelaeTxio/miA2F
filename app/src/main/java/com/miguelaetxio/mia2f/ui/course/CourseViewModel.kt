package com.miguelaetxio.mia2f.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.course.Course
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.course.RefreshResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notice shown on the home screen after a refresh.
 * ---
 * Aviso que se muestra en el inicio tras una renovación.
 */
data class RefreshNotice(val message: String, val isError: Boolean)

/**
 * Exposes the cached course and refreshes it once when created.
 * ---
 * Expone el curso guardado y lo renueva una vez al crearse.
 */
@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val course: StateFlow<Course?> = courseRepository.course

    private val _refreshNotice = MutableStateFlow<RefreshNotice?>(null)

    /**
     * Notice of the last refresh, or null if it went fine.
     * ---
     * Aviso de la última renovación, o null si fue bien.
     */
    val refreshNotice: StateFlow<RefreshNotice?> =
        _refreshNotice.asStateFlow()

    init {
        viewModelScope.launch {
            _refreshNotice.value = toNotice(courseRepository.refresh())
        }
    }

    private fun toNotice(result: RefreshResult): RefreshNotice? =
        when (result) {
            is RefreshResult.Offline -> RefreshNotice(
                message = result.cachedVersion?.let {
                    "Sin conexión: usando el temario guardado " +
                        "(versión $it)."
                } ?: ("Sin conexión: el temario se descargará en " +
                    "cuanto haya Internet."),
                isError = false,
            )
            is RefreshResult.Error -> RefreshNotice(
                message = result.message,
                isError = true,
            )
            RefreshResult.UpToDate, is RefreshResult.Updated -> null
        }
}
