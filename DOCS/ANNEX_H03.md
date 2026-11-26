# MIA2F — ANEXO HITO 03
# La profesora: sesiones con ejercicios y corrección

*Vive en `DOCS/ANNEX_H03.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel («PCH a H03») en S2, tras
verificar en su móvil el último punto de H02. El temario ya llega al
móvil (H02); este hito convierte a Gemini en la profesora que lo
trabaja con el alumno (ver `MASTER_DOCUMENT.md` §1, pieza 2).

---

## OBJETIVO DEL HITO

Que el alumno pueda abrir una **clase** desde el inicio, recibir
ejercicios escritos de la unidad que le toca, contestarlos, recibir
la corrección de Gemini con el método de `FrancaisA2`, y que cada
clase quede **registrada en el móvil** (fecha, unidad, ejercicios,
respuestas, correcciones y errores por concepto).

**Fuera de alcance** (hitos posteriores, no adelantarlos): pruebas de
evaluación continua y criterio de concepto superado (H04), copia en
Drive (H05), pronunciación (H06) y escucha (H07).

---

## CONTEXTO TÉCNICO

- **Punto de partida en el código:** `GeminiRepository` (único punto
  con el identificador del modelo, hoy `gemini-3.5-flash`, y una
  instrucción de corrección de frases sueltas), `GeminiApiService` y
  `GeminiDtos` (`generateContent` REST v1beta con `systemInstruction`
  y `contents`), `CourseRepository` (curso guardado como `Course` con
  unidades y conceptos de identificador estable, ver `CourseDtos.kt`)
  y `MarkdownText` para mostrar texto con formato. La navegación es
  por estados en `AppRoot` de `MainActivity.kt`, no NavGraph.
- **Room aún no está en el proyecto.** Hay que añadirlo con versiones
  compatibles con Kotlin 2.3.10, KSP 2.3.9 y Hilt 2.59.2 (ver nota de
  `RESUMPTION_POINT.md` sobre `multiplatform-markdown-renderer`: no
  subir Kotlin/KSP/Hilt de rebote).
- **Identificadores de concepto:** todo error registrado apunta a un
  `Concept.id` existente en el curso guardado
  (p. ej. `fr-a2.u01.presente-etre-avoir`). Gemini solo puede elegir
  entre los identificadores que se le pasan; la app descarta o marca
  cualquier otro (nunca lo guarda tal cual).
- **Método de corrección:** el de `FrancaisA2`
  (`MiguelaeTxio/FrancaisA2`), directriz §4.8 del documento maestro.
  Se lee del propio repositorio antes de redactar los prompts, nunca
  de memoria.
- **Directriz §4.5:** todo lo relativo a la API de Gemini (salida
  estructurada en JSON, conversación de varios turnos, modelo vigente)
  y a Room se verifica en la documentación oficial en el momento de
  implementarlo.

### Comprobado en S2 (punto 1, 2026-11-26)

- **Documentación oficial:** `ai.google.dev/gemini-api/docs/...`
  redirige ahora a `aistudio.google.com/docs/...`. Índice:
  `https://aistudio.google.com/llms.txt`.
- **API:** la documentación presenta la Interactions API como la
  predeterminada para trabajo nuevo y `generateContent` como API
  heredada **que sigue admitida**, sin fecha de retirada
  (`https://aistudio.google.com/docs/deprecations.md`). Se mantiene
  `generateContent` v1beta, que es lo que ya funciona en la app: cada
  llamada de la profesora lleva todo su contexto, así que no hace
  falta la conversación con estado de Interactions.
- **Salida estructurada en REST**
  (`https://aistudio.google.com/docs/structured-output.md?generate-content=true`
  y `https://aistudio.google.com/api-reference/generate-content/generate-content.md`):
  `generationConfig.responseFormat.text` con `mimeType`
  `"APPLICATION_JSON"` y `schema` (JSON Schema). Es la forma del
  ejemplo REST oficial; se usa esa. Palabras clave admitidas:
  `type` (`string`, `number`, `integer`, `boolean`, `object`,
  `array`, `null`), `title`, `description`, `properties`, `required`,
  `additionalProperties`, `enum`, `format`, `minimum`, `maximum`,
  `items`, `prefixItems`, `minItems`, `maxItems`, `anyOf`, `$ref`.
  `minLength`, `maxLength` y similares se ignoran: se validan en la
  app.
- **Varios turnos:** `contents` con entradas `role` `user`/`model` y
  sus `parts`, en orden.
- **Modelo:** `gemini-3.5-flash` es estable (GA desde 2026-05-19) y
  sin fecha de retirada anunciada; se mantiene. Hay Flash más nuevos
  (3.6, 3.7, 3.8); en 3.6 y posteriores `temperature`, `topP` y
  `topK` se ignoran. Cambiar de modelo es tocar una constante.
- **Método de `FrancaisA2`** (`DOCS/MAINS/FRANCAISA2_MASTER_DOCUMENT.md`
  §4.2, §4.4 y §4.5, y `DOCS/PROGRESS/FRANCAISA2_STUDENT_PROGRESS.md`):
  corrección en castellano, honesta y directa, constructiva y nunca
  condescendiente; no silenciar errores reales para no desanimar; ante
  una duda gramatical genuina no afirmar una regla sin certeza (en la
  app: decir que hay duda en vez de inventar); un error se considera
  recurrente solo tras aparecer en dos sesiones distintas (criterio de
  H04, no de este hito). Los errores se anotan por patrón (auxiliar,
  concordancia del participio, acentos, calcos del español, etc.), que
  en la app se traduce a error asociado a un concepto del temario.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Verificación previa (§4.5) y método.** Comprobar en la
   documentación oficial de la Gemini Developer API: salida
   estructurada (`generationConfig` con `responseMimeType`
   `application/json` y el esquema de respuesta vigente), formato de
   `contents` para varios turnos, y que `gemini-3.5-flash` sigue
   siendo un identificador válido. Enlazar `MiguelaeTxio/FrancaisA2`
   (`add_repo`, lectura) y leer su método de enseñanza y corrección.
   Anotar en este anexo, en «Contexto técnico», lo comprobado y la
   URL de cada fuente.
2. **Registro local con Room.** Añadir Room (versiones verificadas,
   procesador por KSP) y una base de datos con tres tablas:
   `study_session` (id, unidad, inicio, fin), `exercise` (sesión,
   concepto, enunciado, respuesta del alumno, corrección, correcta
   sí/no, orden) y `concept_error` (sesión, ejercicio, concepto,
   descripción breve). Módulo de Hilt para la base de datos y los DAO.
   Criterio: compila en Actions y la app arranca igual que antes.
3. **Qué toca trabajar.** `StudyPlanner` (o nombre equivalente) que,
   a partir del curso guardado y del registro, elige la unidad de la
   clase (la primera con conceptos aún no practicados; si todas lo
   están, la de más errores recientes) y construye el **resumen de
   progreso** que se envía a Gemini: conceptos ya practicados y
   errores de las últimas sesiones con su concepto.
4. **Gemini profesora.** En `GeminiRepository`, dos operaciones con
   salida JSON estructurada:
   - **Generar ejercicios:** recibe los conceptos de la unidad
     (identificador, título, explicación, ejemplos) y el resumen de
     progreso, y devuelve una lista corta de ejercicios escritos
     (enunciado en castellano, concepto que trabaja).
   - **Corregir una respuesta:** recibe el ejercicio, la respuesta y
     los conceptos permitidos, y devuelve: correcta sí/no, frase
     corregida, explicación en castellano y lista de errores con su
     concepto.
   Prompts con el método de `FrancaisA2` (punto 1). El modelo sigue
   definido en un único punto. Toda respuesta que no cumpla el
   esquema se trata como error legible, nunca como cierre en falso.
5. **Pantalla «Clase».** Botón «Empezar clase» en el inicio
   (desactivado sin temario guardado). La pantalla muestra la unidad,
   un ejercicio cada vez con su campo de respuesta, la corrección con
   `MarkdownText` y «Siguiente»; al terminar, un resumen de la clase.
   Cada ejercicio corregido se guarda al momento en Room (si se cierra
   la app a mitad, lo hecho no se pierde). Sin conexión: aviso legible
   como el del inicio, sin texto técnico.
6. **Historial de clases.** Pantalla accesible desde el inicio con la
   lista de clases (fecha, unidad, ejercicios hechos, errores) y el
   detalle de cada una.
7. **Verificación en el móvil de Miguel Ángel.** Una clase completa
   real; comprobar que las correcciones están en castellano, que no
   callan errores, que los errores salen con su concepto, y que tras
   cerrar y abrir la app la clase sigue en el historial.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| S2 | 2026-11-26 | Hito abierto por PCH. Anexo y hoja de ruta creados. |
| S2 | 2026-11-26 | Punto 1 hecho: documentación de Gemini comprobada y método de `FrancaisA2` leído (ver «Comprobado en S2»). |
| S2 | 2026-11-26 | Punto 2 hecho: Room 2.8.5 (última estable, comprobada en `developer.android.com/jetpack/androidx/releases/room`; `room-ktx` ya va dentro de `room-runtime`), compilador por KSP, tablas `study_session`, `exercise` y `concept_error`, DAO y `DatabaseModule` (`b791453`, compilación verde). Esquema sin exportar (`exportSchema = false`) porque CI no puede devolver al repo el JSON que genera. |
| S2 | 2026-11-26 | Punto 3 hecho: `StudyPlanner` elige la unidad (primera con conceptos sin practicar; si no queda ninguna, la de más errores recientes) y redacta el resumen de progreso para Gemini (conceptos practicados y últimos 30 errores con su concepto). |
| S2 | 2026-11-26 | Punto 4 hecho: `GeminiRepository.generateExercises` (5 ejercicios por clase) y `correctAnswer`, con `generationConfig.responseFormat.text` y esquema cuyo `conceptId` es un `enum` con los conceptos de la unidad; la app vuelve a filtrar y descarta lo que no cuadre. Instrucción de profesora con el método de `FrancaisA2`. `TeacherResult` separa «sin conexión» de los demás errores. |
