package com.miguelaetxio.mia2f.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home screen state from the class record.
 * ---
 * Estado del inicio a partir del registro de clases.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    studySessionDao: StudySessionDao,
) : ViewModel() {

    /**
     * True once 3 classes have been finished since the last review.
     * ---
     * True cuando se han terminado 3 clases desde el último repaso.
     */
    val isReviewDue: StateFlow<Boolean> = studySessionDao
        .observeLessonsSinceReview()
        .map { it >= LESSONS_PER_REVIEW }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private companion object {
        const val LESSONS_PER_REVIEW = 3
    }
}
