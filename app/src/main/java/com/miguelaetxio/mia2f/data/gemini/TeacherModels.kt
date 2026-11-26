package com.miguelaetxio.mia2f.data.gemini

import com.miguelaetxio.mia2f.data.speech.DialogueLine

/**
 * Written exercise proposed by Gemini for one concept of the unit.
 * ---
 * Ejercicio escrito propuesto por Gemini para un concepto de la
 * unidad.
 */
data class GeneratedExercise(
    val conceptId: String,
    val prompt: String,
)

/**
 * Gemini's correction of one answer. Every error points to a concept
 * of the unit.
 * ---
 * Corrección de Gemini de una respuesta. Cada error apunta a un
 * concepto de la unidad.
 */
data class AnswerCorrection(
    val isCorrect: Boolean,
    val correctedAnswer: String,
    val explanation: String,
    val errors: List<CorrectionError>,
)

data class CorrectionError(
    val conceptId: String,
    val description: String,
)

/**
 * A listening practice: the dialogue and its comprehension questions
 * (prompt = question in Spanish, with the concept it tests).
 * ---
 * Una práctica de escucha: el diálogo y sus preguntas de comprensión
 * (prompt = pregunta en castellano, con el concepto que pone a
 * prueba).
 */
data class ListeningPractice(
    val lines: List<DialogueLine>,
    val questions: List<GeneratedExercise>,
)

/**
 * Gemini's comment on a recording: what it actually hears, whether it
 * is right, the comment in Spanish and the errors by concept.
 * ---
 * Comentario de Gemini sobre una grabación: lo que oye de verdad, si
 * está bien, el comentario en castellano y los errores por concepto.
 */
data class PronunciationFeedback(
    val heard: String,
    val isCorrect: Boolean,
    val explanation: String,
    val errors: List<CorrectionError>,
)

/**
 * Result of a teacher call. Offline is kept apart so the screen can
 * show a plain message instead of the exception text.
 * ---
 * Resultado de una llamada de la profesora. Sin conexión va aparte
 * para que la pantalla muestre un mensaje legible en vez del texto de
 * la excepción.
 */
sealed class TeacherResult<out T> {
    data class Success<T>(val value: T) : TeacherResult<T>()
    data class Error(val message: String) : TeacherResult<Nothing>()
    object Offline : TeacherResult<Nothing>()
}
