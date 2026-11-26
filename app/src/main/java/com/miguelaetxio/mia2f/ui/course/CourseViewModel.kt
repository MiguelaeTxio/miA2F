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
 * Exposes the cached course and refreshes it once when created.
 * ---
 * Expone el curso guardado y lo renueva una vez al crearse.
 */
@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
) : ViewModel() {

    val course: StateFlow<Course?> = courseRepository.course

    private val _refreshMessage = MutableStateFlow<String?>(null)

    /**
     * Error of the last refresh, or null if it went fine.
     * ---
     * Error de la última renovación, o null si fue bien.
     */
    val refreshMessage: StateFlow<String?> = _refreshMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _refreshMessage.value =
                (courseRepository.refresh() as? RefreshResult.Error)?.message
        }
    }
}
