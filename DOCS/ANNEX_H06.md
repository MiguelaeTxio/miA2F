# MIA2F — ANEXO HITO 06
# Pronunciación

*Vive en `DOCS/ANNEX_H06.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel («PCH a H06») en S3, con H05
pausado y su punto 7 (verificación en los dos móviles) pendiente:
Miguel Ángel prueba todo al final, junto con H03 punto 8 y H04
punto 5.

---

## OBJETIVO DEL HITO

Que el alumno practique la pronunciación con la app: escucha una frase
modelo en francés con la voz del móvil, la repite, se graba su voz y
Gemini comenta **la grabación real** (no una transcripción hecha por
el móvil), en castellano y con el método de `FrancaisA2`: honesto, sin
callar errores y sin inventar reglas. Los fallos quedan registrados por
concepto y cuentan en «Mi progreso» como cualquier otro error.

**Fuera de alcance:** escucha de diálogos con preguntas (H07).

---

## DECISIONES DE PARTIDA (tomadas al abrir el hito, revisables)

- **Audio a Gemini en línea** (dentro de la propia petición
  `generateContent`), sin la API de archivos: una frase dura pocos
  segundos y el límite de petición en línea es de 20 MB.
- **La grabación no se guarda**: se borra en cuanto Gemini responde.
  En Room queda lo mismo que en una clase escrita: la frase, lo que
  Gemini entendió, su comentario y los errores por concepto.
- **Sesión de tipo `pronunciation`** (valor nuevo de `kind`, sin
  cambiar el esquema). Cuenta para el criterio de superación de H04
  como cualquier otra sesión, pero **no** para la cadencia de la prueba
  de repaso (que cuenta solo clases).
- **Frases de la unidad en curso** (la misma que elige
  `StudyPlanner`), propuestas por Gemini con el vocabulario y la
  gramática de esa unidad.

---

## CONTEXTO TÉCNICO

- **Audio en Gemini** (`ai.google.dev/gemini-api/docs/audio`,
  consultado el 2026-11-26): formatos admitidos WAV, MP3, AIFF, AAC,
  OGG y FLAC; petición en línea de hasta 20 MB en total; Gemini reduce
  el audio a 16 Kbps y a un solo canal. Android graba AAC
  directamente con `MediaRecorder`.
- **Gemini en la app:** `GeminiRepository` (punto único, modelo
  `gemini-3.5-flash`, `generateContent` v1beta,
  `generationConfig.responseFormat.text` con esquema). `GeminiDtos`
  solo tiene hoy partes de texto: hace falta añadir la parte de datos
  en línea (audio en base64 con su tipo MIME), comprobando el nombre
  exacto del campo en la referencia REST vigente.
- **Voz del móvil:** `android.speech.tts.TextToSpeech` con `fr-FR`
  (`MASTER_DOCUMENT.md` §2.4). La voz francesa depende de cada móvil:
  hay que detectar si falta y llevar al alumno a instalarla.
- **Temario (`miCursos`, `fr/A2/course.json`):** 57 conceptos (v1), de los
  que solo **2 eran de fonética** («ils ont / ils sont» y «Vocales
  nasales»). Los errores de pronunciación se registran por concepto
  (`concept_error.concept_id`), así que con 2 conceptos casi ningún
  fallo tendría dónde anotarse.
- **Permiso de micrófono:** `RECORD_AUDIO` se pide en tiempo de
  ejecución, la primera vez que el alumno va a grabar, con una
  explicación previa en castellano.

### Comprobado en S3 (documentación oficial, 2026-11-26)

- **`TextToSpeech`** (referencia de Android): `setLanguage` devuelve
  `LANG_MISSING_DATA` (-1) si falta la voz y `LANG_NOT_SUPPORTED` (-2)
  si no hay ese idioma; `setSpeechRate(1.0)` es la velocidad normal y
  0.5 la mitad; `speak(CharSequence, int, Bundle, String)`. Las apps
  que apuntan a Android 11 o superior deben declarar
  `android.intent.action.TTS_SERVICE` en `<queries>` del manifiesto.
- **`MediaRecorder`**: `MediaRecorder(Context)` existe desde la API 31
  y el constructor sin argumentos está obsoleto. Como miA2F tiene
  `minSdk 26`, hay que usar uno u otro según la versión.
  `setMaxDuration` se llama entre `setOutputFormat` y `prepare`; al
  llegar al máximo avisa a `OnInfoListener` con
  `MEDIA_RECORDER_INFO_MAX_DURATION_REACHED` y se para sola.
  `RECORD_AUDIO` se declara en el manifiesto y se pide en tiempo de
  ejecución.
- **Gemini:** tipos de audio admitidos, entre otros, `audio/aac` y
  `audio/m4a`; máximo 20 MB por petición. En `generateContent`, el
  ejemplo REST oficial manda el contenido en línea como `inline_data`
  con `mime_type` y `data` en base64. La guía de audio ya muestra la
  nueva API Interactions; la app sigue con `generateContent` v1beta,
  igual que el resto de llamadas.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Conceptos de fonética del temario.** Contrastar con los
   descriptores de control fonológico del A2 del MCER (y con el resto
   de fuentes ya citadas en `course.json`) qué sonidos y rasgos de
   pronunciación corresponden al nivel. Proponer a Miguel Ángel los
   conceptos de fonética que faltan, con su unidad, y **solo con su
   visto bueno** añadirlos a `miCursos` (validar con
   `tools/validate_course.py` y subir `version` en `index.json` y en
   `course.json`).
2. **Documentación oficial vigente (§4.5):** `TextToSpeech` (idioma
   `fr-FR`, `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED`, acción para
   instalar voces, velocidad de habla); `MediaRecorder` con salida AAC
   y permiso `RECORD_AUDIO`; parte de audio en línea de
   `generateContent` v1beta (nombre exacto de los campos y tipo MIME
   de AAC).
3. **Voz francesa.** Clase única que arranca `TextToSpeech`, comprueba
   el francés, reproduce a velocidad normal y lenta, y se libera al
   salir. Si falta la voz, aviso con botón que lleva a instalarla.
4. **Grabación.** Permiso de micrófono con explicación previa; grabar
   hasta 15 segundos en un archivo temporal; botón para parar; borrar
   el archivo en cuanto se envía.
5. **Gemini.** En `GeminiRepository`: proponer 5 frases modelo de la
   unidad (esquema con el concepto de cada una) y evaluar una
   grabación contra su frase (esquema: lo que se oye, si es correcta,
   comentario en castellano y errores por concepto de la unidad). El
   prompt de evaluación pide basarse solo en el audio y decir si no se
   oye bien en vez de suponer.
6. **Pantalla «Practicar pronunciación».** Botón en el inicio. Por
   frase: escucharla (normal o lenta), grabar, enviar, ver el
   comentario y pasar a la siguiente. Cada evaluación se guarda en Room
   al llegar, como en las clases. Historial con «Pronunciación».
7. **Verificación en el móvil de Miguel Ángel.** Voz francesa
   disponible; permiso de micrófono; una frase bien dicha y otra
   pronunciada mal a propósito, comprobando que el comentario se
   refiere a lo que de verdad se dijo; errores visibles en el
   historial y en «Mi progreso».

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| S3 | 2026-11-26 | Hito abierto por PCH. Anexo y hoja de ruta creados; comprobados los formatos de audio de Gemini y los conceptos de fonética del temario (2 de 57). |
| S3 | 2026-11-26 | Punto 1 hecho (`miCursos` `d04979a`, temario v2): el MCER solo describe inteligibilidad y prosodia en A2; la «Matière sonore» del referencial A2 no está en línea, así que se añade como fuente el *Précis de phonétique* de Inspire 2 (A2, Hachette 2020). Miguel Ángel delega la elección pedagógica. 9 conceptos nuevos: ritmo y acento final y entonación de la pregunta (U1), [y]/[u] (U2), [e]/[ɛ] (U4), liaison (U5), [s]/[z] y [ʃ]/[ʒ] (U7), encadenamiento (U8), [ø]/[œ] y [o]/[ɔ] (U9), r francesa (U10). Total: 11 de 66 conceptos son de fonética. |
| S3 | 2026-11-26 | Punto 2 hecho: ver «Comprobado en S3». |
| S3 | 2026-11-26 | Punto 3 hecho (`aff9f18`): `data/speech/FrenchVoice` (TextToSpeech fr-FR, estados listo / falta la voz / no disponible, velocidad normal o 0.7) y `<queries>` con `TTS_SERVICE` en el manifiesto. |
| S3 | 2026-11-26 | Punto 4 hecho (`461da4f`): `data/speech/VoiceRecorder` (AAC ADTS, `audio/aac`, hasta 15 s, en la caché; `MediaRecorder(Context)` desde la API 31) y `RECORD_AUDIO` en el manifiesto. |
| S3 | 2026-11-26 | Punto 5 hecho (`081cacc`): `GeminiRepository.generatePronunciationSentences` (5 frases de 4 a 12 palabras) y `evaluatePronunciation` (audio en `inline_data`; lo que se oye, si es correcta, comentario y errores por concepto; si no se entiende, lo dice sin suponer). |
| S3 | 2026-11-26 | Punto 6 hecho a falta de verificar (`7465a61`): `ui/pronunciation/` con botón en el inicio; permiso de micrófono con explicación; aviso e instalación si falta la voz; «Reenviar» tras error de conexión; guardado en Room al llegar cada comentario; grabación borrada tras la respuesta; sesión `pronunciation`, que cuenta para los estados de H04 pero no para la cadencia del repaso; copia en Drive marcada como pendiente al terminar; historial «Pronunciación · unidad». |
