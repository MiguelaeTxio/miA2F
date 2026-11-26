package com.miguelaetxio.mia2f.data.gemini

import com.google.gson.Gson
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point to Gemini. The model ID lives only here.
 * `gemini-3.5-flash` is stable and has no announced shutdown date
 * (checked 2026-11-26, see ANNEX_H03).
 * ---
 * Punto único de acceso a Gemini. El identificador del modelo vive
 * solo aquí. `gemini-3.5-flash` es estable y no tiene fecha de
 * retirada anunciada (comprobado el 2026-11-26, ver ANNEX_H03).
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val apiKeyStore: ApiKeyStore,
) {

    private val gson = Gson()

    /**
     * Sends a French sentence and returns Gemini's correction in
     * Spanish, following the FrancaisA2 method. Never throws.
     * ---
     * Envía una frase en francés y devuelve la corrección de Gemini en
     * castellano, con el método de FrancaisA2. Nunca lanza excepciones.
     */
    suspend fun correctSentence(sentence: String): GeminiResult =
        when (val result = call(CORRECTION_INSTRUCTION, sentence, null)) {
            is TeacherResult.Success -> GeminiResult.Success(result.value)
            is TeacherResult.Error -> GeminiResult.Error(result.message)
            TeacherResult.Offline -> GeminiResult.Error(OFFLINE_MESSAGE)
        }

    /**
     * Asks Gemini for the written exercises of a class on the given
     * unit, taking the progress summary into account. Exercises whose
     * concept is not in the unit are dropped. Never throws.
     * ---
     * Pide a Gemini los ejercicios escritos de una clase sobre la
     * unidad, teniendo en cuenta el resumen de progreso. Se descartan
     * los ejercicios cuyo concepto no es de la unidad. Nunca lanza.
     */
    suspend fun generateExercises(
        unit: CourseUnit,
        progressSummary: String,
    ): TeacherResult<List<GeneratedExercise>> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Unidad ${unit.number}: ${unit.title}\n\n" +
            "Objetivos de la unidad:\n" +
            unit.canDo.joinToString("\n") { "- ${it.text}" } + "\n\n" +
            "Conceptos de la unidad:\n" + describeConcepts(unit) + "\n\n" +
            "Progreso del alumno:\n$progressSummary\n\n" +
            "Propón $EXERCISE_COUNT ejercicios escritos breves para esta " +
            "clase. Da prioridad a los conceptos sin practicar y a los " +
            "que tienen errores recientes. Cada ejercicio trabaja un solo " +
            "concepto de la lista y su enunciado va en castellano, con " +
            "las frases o palabras en francés que haga falta. El alumno " +
            "responde escribiendo en francés."
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "exercises" to mapOf(
                    "type" to "array",
                    "minItems" to EXERCISE_COUNT,
                    "maxItems" to EXERCISE_COUNT,
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "conceptId" to mapOf(
                                "type" to "string",
                                "enum" to conceptIds,
                            ),
                            "prompt" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("conceptId", "prompt"),
                    ),
                ),
            ),
            "required" to listOf("exercises"),
        )
        return when (val result = call(TEACHER_INSTRUCTION, prompt, schema)) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, ExercisesJson::class.java)
                val exercises = parsed?.exercises.orEmpty().mapNotNull {
                    val conceptId = it.conceptId
                    val text = it.prompt?.trim()
                    if (conceptId in conceptIds && !text.isNullOrEmpty()) {
                        GeneratedExercise(conceptId!!, text)
                    } else {
                        null
                    }
                }
                if (exercises.isEmpty()) {
                    TeacherResult.Error(UNREADABLE_MESSAGE)
                } else {
                    TeacherResult.Success(exercises)
                }
            }
            is TeacherResult.Error -> result
            TeacherResult.Offline -> TeacherResult.Offline
        }
    }

    /**
     * Asks Gemini for a unit test: exactly one exercise per concept of
     * the unit, in the unit's order. The schema has one required
     * property per concept ID, so none can be missing. Never throws.
     * ---
     * Pide a Gemini un test de unidad: exactamente un ejercicio por
     * concepto de la unidad, en su orden. El esquema tiene una
     * propiedad obligatoria por identificador de concepto, así que no
     * puede faltar ninguno. Nunca lanza.
     */
    suspend fun generateTest(
        unit: CourseUnit,
    ): TeacherResult<List<GeneratedExercise>> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Unidad ${unit.number}: ${unit.title}\n\n" +
            "Conceptos de la unidad:\n" + describeConcepts(unit) + "\n\n" +
            "Prepara un test para comprobar si el alumno ya domina esta " +
            "unidad y puede saltarla. Escribe exactamente un ejercicio " +
            "escrito por concepto, bajo la clave de su identificador. " +
            "Cada ejercicio debe demostrar que el alumno domina el " +
            "concepto, sin pistas que den la respuesta. Enunciado en " +
            "castellano, con las frases o palabras en francés que haga " +
            "falta; el alumno responde escribiendo en francés."
        val schema = mapOf(
            "type" to "object",
            "properties" to conceptIds.associateWith {
                mapOf("type" to "string")
            },
            "required" to conceptIds,
        )
        return when (val result = call(TEACHER_INSTRUCTION, prompt, schema)) {
            is TeacherResult.Success -> {
                val parsed: Map<*, *>? = runCatching {
                    gson.fromJson(result.value, Map::class.java)
                }.getOrNull()
                val exercises = conceptIds.mapNotNull { id ->
                    (parsed?.get(id) as? String)?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { GeneratedExercise(id, it) }
                }
                if (exercises.size != conceptIds.size) {
                    TeacherResult.Error(UNREADABLE_MESSAGE)
                } else {
                    TeacherResult.Success(exercises)
                }
            }
            is TeacherResult.Error -> result
            TeacherResult.Offline -> TeacherResult.Offline
        }
    }

    /**
     * Asks Gemini to correct the student's answer to one exercise.
     * Errors whose concept is not in the unit are dropped. Never
     * throws.
     * ---
     * Pide a Gemini que corrija la respuesta del alumno a un
     * ejercicio. Se descartan los errores cuyo concepto no es de la
     * unidad. Nunca lanza.
     */
    suspend fun correctAnswer(
        unit: CourseUnit,
        exercise: GeneratedExercise,
        answer: String,
    ): TeacherResult<AnswerCorrection> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Conceptos de la unidad:\n" + describeConcepts(unit) +
            "\n\nEjercicio (concepto ${exercise.conceptId}):\n" +
            "${exercise.prompt}\n\n" +
            "Respuesta del alumno:\n$answer\n\n" +
            "Corrige la respuesta. En «correctedAnswer» va la respuesta " +
            "corregida en francés (igual que la del alumno si está bien). " +
            "En «explanation», en castellano, explica cada error; si está " +
            "bien, dilo. En «errors» va un elemento por cada error, con el " +
            "concepto de la lista al que corresponde y una descripción " +
            "breve en castellano. «isCorrect» solo es true si no hay " +
            "ningún error."
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "isCorrect" to mapOf("type" to "boolean"),
                "correctedAnswer" to mapOf("type" to "string"),
                "explanation" to mapOf("type" to "string"),
                "errors" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "conceptId" to mapOf(
                                "type" to "string",
                                "enum" to conceptIds,
                            ),
                            "description" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("conceptId", "description"),
                    ),
                ),
            ),
            "required" to listOf(
                "isCorrect", "correctedAnswer", "explanation", "errors",
            ),
        )
        return when (val result = call(TEACHER_INSTRUCTION, prompt, schema)) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, CorrectionJson::class.java)
                val isCorrect = parsed?.isCorrect
                val corrected = parsed?.correctedAnswer?.trim()
                val explanation = parsed?.explanation?.trim()
                if (isCorrect == null || corrected.isNullOrEmpty() ||
                    explanation.isNullOrEmpty()
                ) {
                    TeacherResult.Error(UNREADABLE_MESSAGE)
                } else {
                    val errors = parsed?.errors.orEmpty().mapNotNull {
                        val conceptId = it.conceptId
                        val description = it.description?.trim()
                        if (conceptId in conceptIds &&
                            !description.isNullOrEmpty()
                        ) {
                            CorrectionError(conceptId!!, description)
                        } else {
                            null
                        }
                    }
                    TeacherResult.Success(
                        AnswerCorrection(
                            // Con errores no puede contar como correcta.
                            // ---
                            // With errors it cannot count as correct.
                            isCorrect = isCorrect && errors.isEmpty(),
                            correctedAnswer = corrected,
                            explanation = explanation,
                            errors = errors,
                        )
                    )
                }
            }
            is TeacherResult.Error -> result
            TeacherResult.Offline -> TeacherResult.Offline
        }
    }

    private fun describeConcepts(unit: CourseUnit): String =
        unit.concepts.joinToString("\n") { concept ->
            "- ${concept.id} — ${concept.title}: ${concept.explanation}" +
                if (concept.examples.isEmpty()) {
                    ""
                } else {
                    " Ejemplos: " + concept.examples.joinToString(" / ")
                }
        }

    private fun <T> parse(json: String, type: Class<T>): T? =
        runCatching { gson.fromJson(json, type) }.getOrNull()

    /**
     * One generateContent call. With a schema, asks for JSON through
     * generationConfig.responseFormat.text. Returns the text of the
     * first candidate.
     * ---
     * Una llamada a generateContent. Con esquema, pide JSON mediante
     * generationConfig.responseFormat.text. Devuelve el texto del
     * primer candidato.
     */
    private suspend fun call(
        instruction: String,
        userText: String,
        schema: Map<String, Any>?,
    ): TeacherResult<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyStore.readKey()
            ?: return@withContext TeacherResult.Error(
                "No hay ninguna clave de Gemini guardada."
            )
        val request = GenerateContentRequest(
            systemInstruction = Content(
                parts = listOf(Part(text = instruction)),
            ),
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = userText))),
            ),
            generationConfig = schema?.let {
                GenerationConfig(
                    responseFormat = ResponseFormat(
                        text = TextResponseFormat(
                            mimeType = "APPLICATION_JSON",
                            schema = it,
                        ),
                    ),
                )
            },
        )
        try {
            val response =
                geminiApiService.generateContent(MODEL_ID, apiKey, request)
            if (!response.isSuccessful) {
                val apiMessage = response.errorBody()?.string()?.let {
                    runCatching {
                        gson.fromJson(it, GeminiErrorEnvelope::class.java)
                            ?.error?.message
                    }.getOrNull()
                }
                return@withContext TeacherResult.Error(
                    "Error ${response.code()}: " +
                        (apiMessage ?: "respuesta no válida de Gemini.")
                )
            }
            val text = response.body()?.candidates
                ?.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()
            if (text.isNullOrEmpty()) {
                TeacherResult.Error("Gemini no ha devuelto texto.")
            } else {
                TeacherResult.Success(text)
            }
        } catch (e: IOException) {
            TeacherResult.Offline
        } catch (e: Exception) {
            TeacherResult.Error(
                "No se pudo conectar con Gemini: ${e.message}"
            )
        }
    }

    private class ExercisesJson(val exercises: List<ExerciseJson>?)

    private class ExerciseJson(val conceptId: String?, val prompt: String?)

    private class CorrectionJson(
        val isCorrect: Boolean?,
        val correctedAnswer: String?,
        val explanation: String?,
        val errors: List<ErrorJson>?,
    )

    private class ErrorJson(val conceptId: String?, val description: String?)

    private companion object {
        const val MODEL_ID = "gemini-3.5-flash"
        const val EXERCISE_COUNT = 5
        const val OFFLINE_MESSAGE =
            "Sin conexión: hace falta Internet para hablar con Gemini."
        const val UNREADABLE_MESSAGE =
            "Gemini ha devuelto una respuesta que no se puede leer. " +
                "Vuelve a intentarlo."

        // Método de corrección de FrancaisA2 (MASTER_DOCUMENT §4.8).
        // ---
        // FrancaisA2 correction method (MASTER_DOCUMENT §4.8).
        const val CORRECTION_INSTRUCTION =
            "Eres profesor de francés de un alumno hispanohablante de " +
                "nivel A2. Corrige la frase en francés que te envía. " +
                "Responde en castellano: primero la frase corregida, " +
                "después cada error con una explicación breve. Sé " +
                "honesto y directo: no silencies errores ni inventes " +
                "reglas. Si la frase es correcta, dilo."

        // Método de FrancaisA2 aplicado a la clase (ANNEX_H03,
        // «Comprobado en S2»).
        // ---
        // FrancaisA2 method applied to the class (ANNEX_H03).
        const val TEACHER_INSTRUCTION =
            "Eres la profesora de francés de un alumno hispanohablante " +
                "que sigue un curso de nivel A2 del MCER. Hablas con él " +
                "en castellano y le pones ejercicios en francés. Corriges " +
                "con honestidad directa, de forma constructiva y nunca " +
                "condescendiente: no silencias ningún error real para no " +
                "desanimarle, porque eso no le ayuda. No inventas reglas " +
                "gramaticales: si dudas de una regla o de un uso, lo dices " +
                "en vez de afirmarlo. Te ciñes a los conceptos de la " +
                "unidad que se te dan y respondes solo con el JSON pedido."
    }
}

/**
 * Result of a Gemini call.
 * ---
 * Resultado de una llamada a Gemini.
 */
sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}
