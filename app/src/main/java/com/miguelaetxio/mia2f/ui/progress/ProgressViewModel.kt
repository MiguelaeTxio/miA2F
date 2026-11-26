package com.miguelaetxio.mia2f.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.course.Concept
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.study.ConceptProgress
import com.miguelaetxio.mia2f.data.study.ConceptProgressCalculator
import com.miguelaetxio.mia2f.data.study.ConceptStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One concept of a unit with its state.
 * ---
 * Un concepto de una unidad con su estado.
 */
data class ConceptRow(
    val concept: Concept,
    val progress: ConceptProgress,
)

/**
 * One unit with the state of each of its concepts.
 * ---
 * Una unidad con el estado de cada uno de sus conceptos.
 */
data class UnitProgress(
    val unit: CourseUnit,
    val rows: List<ConceptRow>,
) {
    fun count(status: ConceptStatus): Int =
        rows.count { it.progress.status == status }
}

/**
 * Computes the state of every concept, unit by unit, each time the
 * screen opens and whenever the course changes.
 * ---
 * Calcula el estado de cada concepto, unidad por unidad, cada vez que
 * se abre la pantalla y cuando cambia el curso.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    courseRepository: CourseRepository,
    private val conceptProgressCalculator: ConceptProgressCalculator,
) : ViewModel() {

    private val _units = MutableStateFlow<List<UnitProgress>?>(null)

    /**
     * Units with their concept states; null while computing.
     * ---
     * Unidades con el estado de sus conceptos; null mientras se
     * calcula.
     */
    val units: StateFlow<List<UnitProgress>?> = _units.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.course.filterNotNull().collect { course ->
                val progress = conceptProgressCalculator.calculate(course)
                _units.value = course.units
                    .sortedBy { it.number }
                    .map { unit ->
                        UnitProgress(
                            unit = unit,
                            rows = unit.concepts.mapNotNull { concept ->
                                progress[concept.id]?.let {
                                    ConceptRow(concept, it)
                                }
                            },
                        )
                    }
            }
        }
    }
}
