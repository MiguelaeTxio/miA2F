package com.miguelaetxio.mia2f.data.gemini

import com.google.gson.Gson
import com.miguelaetxio.mia2f.data.course.Concept
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.LogArea
import com.miguelaetxio.mia2f.data.settings.ApiKeyStore
import com.miguelaetxio.mia2f.data.speech.DialogueLine
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
        when (
            val result = call(
                CORRECTION_INSTRUCTION, sentence, null, "prueba de conexión",
            )
        ) {
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
            "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) + "\n\n" +
            "Progreso del alumno:\n$progressSummary\n\n" +
            "Propón $EXERCISE_COUNT ejercicios escritos breves para esta " +
            "clase. Da prioridad a los conceptos de la unidad que están " +
            "en observación, insistiendo en sus errores recientes; " +
            "después, a los que el alumno aún no ha trabajado. Cada " +
            "ejercicio trabaja un solo " +
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
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "ejercicios de clase",
            )
        ) {
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
                    unreadable("ejercicios de clase", result.value)
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
     * the unit, in the unit's order. Never throws.
     * ---
     * Pide a Gemini un test de unidad: exactamente un ejercicio por
     * concepto de la unidad, en su orden. Nunca lanza.
     */
    suspend fun generateTest(
        unit: CourseUnit,
    ): TeacherResult<List<GeneratedExercise>> = generatePerConcept(
        concepts = unit.concepts,
        prompt = "Unidad ${unit.number}: ${unit.title}\n\n" +
            "Conceptos de la unidad:\n" + describeConcepts(unit.concepts) +
            "\n\nPrepara un test para comprobar si el alumno ya domina " +
            "esta unidad y puede saltarla. $PER_CONCEPT_RULES",
    )

    /**
     * Asks Gemini for a review (continuous assessment): exactly one
     * exercise per given concept, which may come from several units.
     * Never throws.
     * ---
     * Pide a Gemini una prueba de repaso (evaluación continua):
     * exactamente un ejercicio por concepto dado, que pueden ser de
     * varias unidades. Nunca lanza.
     */
    suspend fun generateReview(
        concepts: List<Concept>,
    ): TeacherResult<List<GeneratedExercise>> = generatePerConcept(
        concepts = concepts,
        prompt = "Conceptos que hay que repasar:\n" +
            describeConcepts(concepts) +
            "\n\nPrepara una prueba de repaso de evaluación continua " +
            "para comprobar si el alumno sigue dominando estos " +
            "conceptos ya trabajados. $PER_CONCEPT_RULES",
    )

    /**
     * One exercise per concept, in the given order. The schema has one
     * required property per concept ID, so none can be missing.
     * ---
     * Un ejercicio por concepto, en el orden dado. El esquema tiene una
     * propiedad obligatoria por identificador, así que no falta
     * ninguno.
     */
    private suspend fun generatePerConcept(
        concepts: List<Concept>,
        prompt: String,
    ): TeacherResult<List<GeneratedExercise>> {
        val conceptIds = concepts.map { it.id }
        val schema = mapOf(
            "type" to "object",
            "properties" to conceptIds.associateWith {
                mapOf("type" to "string")
            },
            "required" to conceptIds,
        )
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "test o repaso",
            )
        ) {
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
                    unreadable("test o repaso", result.value)
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
        val prompt = "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) +
            "\n\nEjercicio (concepto ${exercise.conceptId}):\n" +
            "${exercise.prompt}\n\n" +
            "Respuesta del alumno:\n$answer\n\n" +
            "Corrige la respuesta. En «correctedAnswer» va la respuesta " +
            "corregida en francés (igual que la del alumno si está bien). " +
            "En «explanation», en castellano, explica cada error; si está " +
            "bien, dilo. En «errors» va un elemento por cada error, con el " +
            "concepto de la lista al que corresponde y una descripción " +
            "breve en castellano. «isCorrect» solo es true si no hay " +
            "ningún error. $MINOR_SLIPS_RULE"
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
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "corrección de ejercicio",
            )
        ) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, CorrectionJson::class.java)
                val isCorrect = parsed?.isCorrect
                val corrected = parsed?.correctedAnswer?.trim()
                val explanation = parsed?.explanation?.trim()
                if (isCorrect == null || corrected.isNullOrEmpty() ||
                    explanation.isNullOrEmpty()
                ) {
                    unreadable("corrección de ejercicio", result.value)
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

    /**
     * Asks Gemini for the model sentences of a pronunciation practice
     * on the unit: short, natural, with the unit's vocabulary and
     * grammar, favouring its phonetics concepts. Never throws.
     * ---
     * Pide a Gemini las frases modelo de una práctica de pronunciación
     * de la unidad: cortas, naturales, con el vocabulario y la
     * gramática de la unidad, dando preferencia a sus conceptos de
     * fonética. Nunca lanza.
     */
    suspend fun generatePronunciationSentences(
        unit: CourseUnit,
        progressSummary: String,
    ): TeacherResult<List<GeneratedExercise>> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Unidad ${unit.number}: ${unit.title}\n\n" +
            "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) + "\n\n" +
            "Progreso del alumno:\n$progressSummary\n\n" +
            "Propón $SENTENCE_COUNT frases en francés para practicar la " +
            "pronunciación: el alumno las escuchará y las repetirá en voz " +
            "alta. Cada frase, de 4 a 12 palabras, natural y propia de " +
            "un A2, con el vocabulario y la gramática de la unidad. Da " +
            "prioridad a los conceptos de fonética (phonetics) de la " +
            "lista y a los que están en observación. En «conceptId» va " +
            "el concepto de la lista que más trabaja la frase y en " +
            "«sentence», solo la frase en francés."
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "sentences" to mapOf(
                    "type" to "array",
                    "minItems" to SENTENCE_COUNT,
                    "maxItems" to SENTENCE_COUNT,
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "conceptId" to mapOf(
                                "type" to "string",
                                "enum" to conceptIds,
                            ),
                            "sentence" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("conceptId", "sentence"),
                    ),
                ),
            ),
            "required" to listOf("sentences"),
        )
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "frases de pronunciación",
            )
        ) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, SentencesJson::class.java)
                val sentences = parsed?.sentences.orEmpty().mapNotNull {
                    val conceptId = it.conceptId
                    val text = it.sentence?.trim()
                    if (conceptId in conceptIds && !text.isNullOrEmpty()) {
                        GeneratedExercise(conceptId!!, text)
                    } else {
                        null
                    }
                }
                if (sentences.isEmpty()) {
                    unreadable("frases de pronunciación", result.value)
                } else {
                    TeacherResult.Success(sentences)
                }
            }
            is TeacherResult.Error -> result
            TeacherResult.Offline -> TeacherResult.Offline
        }
    }

    /**
     * Sends the student's recording of a model sentence and returns
     * Gemini's comment on the real audio. Errors whose concept is not
     * in the unit are dropped. Never throws.
     * ---
     * Envía la grabación del alumno de una frase modelo y devuelve el
     * comentario de Gemini sobre el audio real. Se descartan los
     * errores cuyo concepto no es de la unidad. Nunca lanza.
     */
    suspend fun evaluatePronunciation(
        unit: CourseUnit,
        sentence: GeneratedExercise,
        audioBase64: String,
        audioMimeType: String,
    ): TeacherResult<PronunciationFeedback> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) +
            "\n\nFrase modelo (concepto ${sentence.conceptId}):\n" +
            "${sentence.prompt}\n\n" +
            "El audio adjunto es la grabación del alumno diciendo esa " +
            "frase. Básate solo en lo que se oye en el audio. En «heard» " +
            "escribe en francés lo que realmente se oye. «isCorrect» es " +
            "true solo si la pronunciación es clara y correcta para un " +
            "A2. En «explanation», en castellano, comenta cada sonido, " +
            "liaison, encadenamiento o entonación que no esté bien, y " +
            "cómo decirlo; si está bien, dilo. En «errors», un elemento " +
            "por cada error, con el concepto de la lista al que " +
            "corresponde (preferentemente uno de fonética) y una " +
            "descripción breve en castellano. Si el audio está vacío, " +
            "cortado o no se entiende, dilo en «explanation», deja " +
            "«errors» vacío e «isCorrect» en false: no supongas lo que " +
            "no se oye."
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "heard" to mapOf("type" to "string"),
                "isCorrect" to mapOf("type" to "boolean"),
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
                "heard", "isCorrect", "explanation", "errors",
            ),
        )
        val audio = InlineData(mimeType = audioMimeType, data = audioBase64)
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema,
                "evaluación de pronunciación", audio,
            )
        ) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, FeedbackJson::class.java)
                val heard = parsed?.heard?.trim()
                val isCorrect = parsed?.isCorrect
                val explanation = parsed?.explanation?.trim()
                if (heard == null || isCorrect == null ||
                    explanation.isNullOrEmpty()
                ) {
                    unreadable("evaluación de pronunciación", result.value)
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
                        PronunciationFeedback(
                            heard = heard,
                            isCorrect = isCorrect && errors.isEmpty(),
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

    /**
     * Asks Gemini for a listening practice on the unit: a short A2
     * dialogue between two speakers and comprehension questions in
     * Spanish, each tied to a concept of the unit. Never throws.
     * ---
     * Pide a Gemini una práctica de escucha de la unidad: un diálogo
     * corto de A2 entre dos interlocutores y preguntas de comprensión
     * en castellano, cada una ligada a un concepto de la unidad. Nunca
     * lanza.
     */
    suspend fun generateListening(
        unit: CourseUnit,
        progressSummary: String,
    ): TeacherResult<ListeningPractice> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Unidad ${unit.number}: ${unit.title}\n\n" +
            "Objetivos de la unidad:\n" +
            unit.canDo.joinToString("\n") { "- ${it.text}" } + "\n\n" +
            "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) + "\n\n" +
            "Progreso del alumno:\n$progressSummary\n\n" +
            "Escribe un diálogo en francés entre dos personas (A y B), " +
            "de $MIN_LINES a $MAX_LINES réplicas cortas, natural y propio " +
            "de un A2, sobre una situación de la unidad y con su " +
            "vocabulario y gramática. El alumno lo escuchará sin ver el " +
            "texto. Después escribe $QUESTION_COUNT preguntas de " +
            "comprensión en castellano que se respondan con lo que se " +
            "oye en el diálogo, cada una con el concepto de la lista " +
            "que pone a prueba, dando prioridad a los que están en " +
            "observación. El alumno responderá en castellano."
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "dialogue" to mapOf(
                    "type" to "array",
                    "minItems" to MIN_LINES,
                    "maxItems" to MAX_LINES,
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "speaker" to mapOf(
                                "type" to "string",
                                "enum" to listOf("A", "B"),
                            ),
                            "text" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("speaker", "text"),
                    ),
                ),
                "questions" to mapOf(
                    "type" to "array",
                    "minItems" to QUESTION_COUNT,
                    "maxItems" to QUESTION_COUNT,
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "conceptId" to mapOf(
                                "type" to "string",
                                "enum" to conceptIds,
                            ),
                            "question" to mapOf("type" to "string"),
                        ),
                        "required" to listOf("conceptId", "question"),
                    ),
                ),
            ),
            "required" to listOf("dialogue", "questions"),
        )
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "diálogo de escucha",
            )
        ) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, ListeningJson::class.java)
                val lines = parsed?.dialogue.orEmpty().mapNotNull {
                    val text = it.text?.trim()
                    if (text.isNullOrEmpty()) {
                        null
                    } else {
                        DialogueLine(if (it.speaker == "B") 1 else 0, text)
                    }
                }
                val questions = parsed?.questions.orEmpty().mapNotNull {
                    val conceptId = it.conceptId
                    val text = it.question?.trim()
                    if (conceptId in conceptIds && !text.isNullOrEmpty()) {
                        GeneratedExercise(conceptId!!, text)
                    } else {
                        null
                    }
                }
                if (lines.size < 2 || questions.isEmpty()) {
                    unreadable("diálogo de escucha", result.value)
                } else {
                    TeacherResult.Success(ListeningPractice(lines, questions))
                }
            }
            is TeacherResult.Error -> result
            TeacherResult.Offline -> TeacherResult.Offline
        }
    }

    /**
     * Corrects the answer (in Spanish) to one comprehension question,
     * with the whole dialogue as reference. correctedAnswer is the
     * expected answer. Never throws.
     * ---
     * Corrige la respuesta (en castellano) a una pregunta de
     * comprensión, con el diálogo completo como referencia.
     * correctedAnswer es la respuesta esperada. Nunca lanza.
     */
    suspend fun correctListeningAnswer(
        unit: CourseUnit,
        transcript: String,
        question: GeneratedExercise,
        answer: String,
    ): TeacherResult<AnswerCorrection> {
        val conceptIds = unit.concepts.map { it.id }
        val prompt = "Conceptos de la unidad:\n" +
            describeConcepts(unit.concepts) +
            "\n\nDiálogo que escuchó el alumno:\n$transcript\n\n" +
            "Pregunta de comprensión (concepto ${question.conceptId}):\n" +
            "${question.prompt}\n\n" +
            "Respuesta del alumno, en castellano:\n$answer\n\n" +
            "Juzga solo si el alumno ha entendido lo que se dice en el " +
            "diálogo, no su ortografía en castellano. En " +
            "«correctedAnswer» va la respuesta correcta en castellano. " +
            "En «explanation», en castellano, explica qué se dice en el " +
            "diálogo y, si el alumno se ha equivocado, qué parte no " +
            "entendió, citando la frase en francés. En «errors», un " +
            "elemento por cada malentendido, con el concepto de la " +
            "lista al que corresponde y una descripción breve en " +
            "castellano. «isCorrect» solo es true si ha entendido bien."
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
        return when (
            val result = call(
                TEACHER_INSTRUCTION, prompt, schema, "corrección de escucha",
            )
        ) {
            is TeacherResult.Success -> {
                val parsed = parse(result.value, CorrectionJson::class.java)
                val isCorrect = parsed?.isCorrect
                val corrected = parsed?.correctedAnswer?.trim()
                val explanation = parsed?.explanation?.trim()
                if (isCorrect == null || corrected.isNullOrEmpty() ||
                    explanation.isNullOrEmpty()
                ) {
                    unreadable("corrección de escucha", result.value)
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

    private fun describeConcepts(concepts: List<Concept>): String =
        concepts.joinToString("\n") { concept ->
            "- ${concept.id} — ${concept.title}: ${concept.explanation}" +
                if (concept.examples.isEmpty()) {
                    ""
                } else {
                    " Ejemplos: " + concept.examples.joinToString(" / ")
                }
        }

    /**
     * Logs a response that could not be read (its start) and returns
     * the message for the student.
     * ---
     * Registra una respuesta que no se ha podido leer (su comienzo) y
     * devuelve el mensaje para el alumno.
     */
    private fun unreadable(label: String, raw: String): TeacherResult.Error {
        AppLog.log(
            LogArea.GEMINI,
            "$label: respuesta ilegible — ${raw.take(LOG_EXCERPT)}",
        )
        return TeacherResult.Error(UNREADABLE_MESSAGE)
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
        label: String,
        audio: InlineData? = null,
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
                Content(
                    role = "user",
                    parts = listOfNotNull(
                        Part(text = userText),
                        audio?.let { Part(inlineData = it) },
                    ),
                ),
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
        val started = System.currentTimeMillis()
        val sizes = "texto ${userText.length} car." +
            (audio?.let { ", audio ${it.data.length} car. base64" } ?: "")
        fun elapsed() = "${System.currentTimeMillis() - started} ms"
        try {
            val response =
                geminiApiService.generateContent(MODEL_ID, apiKey, request)
            if (!response.isSuccessful) {
                val body = response.errorBody()?.string()
                val apiMessage = body?.let {
                    runCatching {
                        gson.fromJson(it, GeminiErrorEnvelope::class.java)
                            ?.error?.message
                    }.getOrNull()
                }
                AppLog.log(
                    LogArea.GEMINI,
                    "$label: HTTP ${response.code()} en ${elapsed()} " +
                        "($sizes) — " +
                        (body?.take(LOG_EXCERPT) ?: "sin cuerpo"),
                )
                return@withContext TeacherResult.Error(
                    "Error ${response.code()}: " +
                        (apiMessage ?: "respuesta no válida de Gemini.")
                )
            }
            val candidate = response.body()?.candidates?.firstOrNull()
            val text = candidate?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()
            AppLog.log(
                LogArea.GEMINI,
                "$label: OK en ${elapsed()} ($sizes), respuesta " +
                    "${text?.length ?: 0} car., finishReason " +
                    "${candidate?.finishReason}",
            )
            if (text.isNullOrEmpty()) {
                TeacherResult.Error("Gemini no ha devuelto texto.")
            } else {
                TeacherResult.Success(text)
            }
        } catch (e: IOException) {
            AppLog.log(
                LogArea.GEMINI,
                "$label: sin conexión tras ${elapsed()} — " +
                    "${e::class.java.simpleName}: ${e.message}",
            )
            TeacherResult.Offline
        } catch (e: Exception) {
            AppLog.error(LogArea.GEMINI, "$label: fallo tras ${elapsed()}", e)
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

    private class ListeningJson(
        val dialogue: List<LineJson>?,
        val questions: List<QuestionJson>?,
    )

    private class LineJson(val speaker: String?, val text: String?)

    private class QuestionJson(val conceptId: String?, val question: String?)

    private class SentencesJson(val sentences: List<SentenceJson>?)

    private class SentenceJson(val conceptId: String?, val sentence: String?)

    private class FeedbackJson(
        val heard: String?,
        val isCorrect: Boolean?,
        val explanation: String?,
        val errors: List<ErrorJson>?,
    )

    private companion object {
        const val MODEL_ID = "gemini-3.5-flash"
        const val EXERCISE_COUNT = 5
        const val SENTENCE_COUNT = 5

        // Petición de Miguel Ángel (S3): los despistes de escritura no
        // bloquean. Se comentan para no callarlos, pero no cuentan.
        // ---
        // Miguel Ángel's request (S3): typing slips do not block. They
        // are mentioned, so nothing is hidden, but they do not count.
        const val MINOR_SLIPS_RULE =
            "Los despistes de escritura NO son errores: puntuación " +
                "(punto final, comas, signos), guiones, mayúsculas, " +
                "espacios, comillas o apóstrofos tipográficos, y una " +
                "letra cambiada, repetida o comida por despiste cuando " +
                "no forma otra palabra ni cambia la gramática. No van " +
                "en «errors» ni hacen falsa «isCorrect»; menciónalos al " +
                "final de «explanation» como «Detalle de escritura». Sí " +
                "son errores las tildes que faltan o sobran y cualquier " +
                "fallo de gramática, vocabulario o concordancia."
        const val LOG_EXCERPT = 1000
        const val MIN_LINES = 6
        const val MAX_LINES = 10
        const val QUESTION_COUNT = 4
        const val PER_CONCEPT_RULES =
            "Escribe exactamente un ejercicio escrito por concepto, bajo " +
                "la clave de su identificador. Cada ejercicio debe " +
                "demostrar que el alumno domina el concepto, sin pistas " +
                "que den la respuesta. Enunciado en castellano, con las " +
                "frases o palabras en francés que haga falta; el alumno " +
                "responde escribiendo en francés."
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
