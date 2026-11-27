package com.miguelaetxio.mia2f.data.study

import com.miguelaetxio.mia2f.data.gemini.AnswerCorrection
import com.miguelaetxio.mia2f.data.gemini.CorrectionError
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.LogArea
import javax.inject.Inject

/**
 * Miguel Ángel's rule for writing slips (S3): a slip is a minor fault
 * the first two times; the third time the same type comes up within
 * the last REPEAT_WINDOW answers it is serious, counts as an error of
 * the exercise's concept and makes the exercise wrong. The decision is
 * made here, from what Room holds, not by Gemini.
 * ---
 * Regla de Miguel Ángel para los despistes de escritura (S3): un
 * despiste es falta leve las dos primeras veces; la tercera vez que
 * sale el mismo tipo dentro de las últimas REPEAT_WINDOW respuestas es
 * grave, cuenta como error del concepto del ejercicio y deja el
 * ejercicio mal. Lo decide la app con lo que hay en Room, no Gemini.
 */
class WritingSlipPolicy @Inject constructor(
    private val writingSlipDao: WritingSlipDao,
) {

    /**
     * Records the slips of the answer and returns the correction with
     * the slip notes appended and, if one was serious, its error.
     * ---
     * Registra los despistes de la respuesta y devuelve la corrección
     * con las notas de los despistes al final y, si alguno fue grave,
     * su error.
     */
    suspend fun apply(
        exercise: Exercise,
        correction: AnswerCorrection,
    ): AnswerCorrection {
        if (correction.slips.isEmpty()) return correction
        val notes = mutableListOf<String>()
        val serious = mutableListOf<CorrectionError>()
        val byType = correction.slips.groupBy { it.type }
        val rows = byType.map { (type, list) ->
            val description = list.joinToString("; ") { it.description }
            val previous = writingSlipDao.countRecent(
                type.name, exercise.id, REPEAT_WINDOW,
            )
            val time = previous + 1
            val isSerious = time >= SERIOUS_AT
            if (isSerious) {
                notes.add(
                    "**Falta grave por reincidencia** (${type.label}, " +
                        "ya van $time veces): $description"
                )
                serious.add(
                    CorrectionError(
                        exercise.conceptId,
                        "Despiste reiterado de ${type.label}: $description",
                    )
                )
            } else {
                notes.add(
                    "Detalle de escritura (${type.label}, aviso $time de " +
                        "$SERIOUS_AT): $description"
                )
            }
            WritingSlip(
                sessionId = exercise.sessionId,
                exerciseId = exercise.id,
                type = type.name,
                description = description,
                isSerious = isSerious,
            )
        }
        writingSlipDao.insertAll(rows)
        AppLog.log(
            LogArea.STUDY,
            "Despistes en el ejercicio ${exercise.id}: " +
                rows.joinToString {
                    it.type + if (it.isSerious) " (grave)" else " (leve)"
                },
        )
        return correction.copy(
            isCorrect = correction.isCorrect && serious.isEmpty(),
            explanation = correction.explanation + "\n\n" +
                notes.joinToString("\n\n"),
            errors = correction.errors + serious,
        )
    }

    companion object {

        // La tercera vez del mismo tipo ya es grave.
        // ---
        // The third time of the same type is serious.
        const val SERIOUS_AT = 3

        // Reincidencia: dentro de las últimas 20 respuestas escritas.
        // ---
        // Repetition: within the last 20 written answers.
        const val REPEAT_WINDOW = 20
    }
}
