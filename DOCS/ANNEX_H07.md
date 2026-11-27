# MIA2F — ANEXO HITO 07
# Escucha

*Vive en `DOCS/ANNEX_H07.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel («PCH a H07») en S3, con H06
pausado y su punto 7 (verificación en el móvil) pendiente: Miguel
Ángel prueba todo al final, junto con H03 punto 8, H04 punto 5 y H05
punto 7. Es el último hito de la hoja de ruta estratégica.

---

## OBJETIVO DEL HITO

Que el alumno entrene la comprensión oral: escucha un diálogo corto en
francés de su unidad, leído por la voz del móvil, sin ver el texto, y
contesta preguntas de comprensión que corrige Gemini con el método de
`FrancaisA2`. Los fallos quedan registrados por concepto y cuentan en
«Mi progreso».

---

## DECISIONES DE PARTIDA (tomadas al abrir el hito, revisables)

- **Diálogo de la unidad en curso** (la que elige `StudyPlanner`),
  escrito por Gemini con el vocabulario y la gramática de esa unidad:
  dos interlocutores, de 6 a 10 réplicas, A2.
- **Dos voces distintas** si el móvil tiene más de una voz francesa;
  si solo tiene una, se distinguen cambiando el tono. Velocidad normal
  y lenta, y se puede escuchar tantas veces como se quiera.
- **El texto del diálogo no se ve** hasta terminar las preguntas; al
  final se muestra la transcripción.
- **Preguntas en castellano y respuestas en castellano**: se mide la
  comprensión, no la expresión escrita (eso ya lo hacen las clases).
  4 preguntas, cada una ligada al concepto de la unidad que pone a
  prueba.
- **Sesión de tipo `listening`** (valor nuevo de `kind`, sin cambio de
  esquema). Cuenta para los estados de H04, no para la cadencia del
  repaso, igual que la pronunciación.

---

## CONTEXTO TÉCNICO

- **Voz:** `data/speech/FrenchVoice` (H06) ya arranca `TextToSpeech`
  con `fr-FR`, detecta si falta la voz y habla a velocidad normal o
  0.7. Hay que ampliarla para leer varias réplicas seguidas, con voz o
  tono por interlocutor, y saber cuándo termina o pararla.
- **Gemini:** `GeminiRepository` (salida estructurada con esquema);
  patrón de `generateExercises`/`correctAnswer` y de la pronunciación.
- **Registro:** Room `study.db` v2; cada pregunta es un `exercise`,
  cada error un `concept_error`. No hay tabla para el diálogo: la
  transcripción tiene que quedar en el `prompt` de los ejercicios para
  que el historial la muestre.
- **Copia en Drive (H05):** al terminar, `markPending()`.

### Comprobado en S3 (referencia de Android, 2026-11-26)

- `getVoices()` devuelve el conjunto de voces del motor (puede haber
  varias por idioma); `setVoice(Voice)` admite una de ellas y devuelve
  `SUCCESS` o `ERROR`. Cada `Voice` dice su idioma (`getLocale`), su
  nombre único (`getName`) y si necesita red
  (`isNetworkConnectionRequired`).
- `setPitch(1.0)` es el tono normal; valores menores lo bajan.
- `QUEUE_ADD` añade al final de la cola de reproducción.
- `UtteranceProgressListener`: `onStart`, `onDone` (todo el audio ya
  sonó), `onError(String, int)` y `onStop` (al parar o al vaciar la
  cola con `QUEUE_FLUSH`). Sus avisos pueden llegar desde varios
  hilos.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Documentación oficial vigente (§4.5):** `TextToSpeech.getVoices()`
   y `setVoice()` (cómo elegir dos voces `fr-FR` y qué pasa si una
   necesita red), `setPitch()`, `QUEUE_ADD` y
   `UtteranceProgressListener` (saber cuándo acaba la última réplica).
2. **Voz de diálogo.** Ampliar `FrenchVoice`: leer una lista de
   réplicas con dos voces (o dos tonos), a velocidad normal o lenta;
   estado «sonando» y botón para parar.
3. **Gemini.** En `GeminiRepository`: generar el diálogo (réplicas con
   interlocutor A/B) y 4 preguntas de comprensión en castellano, cada
   una con su concepto de la unidad; y corregir la respuesta a una
   pregunta (correcta o no, explicación en castellano y errores por
   concepto), dándole el diálogo completo.
4. **Pantalla «Escuchar un diálogo».** Botón en el inicio. Escuchar
   (normal o despacio, sin límite) sin ver el texto; contestar las
   preguntas una a una con corrección inmediata; al final,
   transcripción y resumen. Cada corrección se guarda en Room al
   llegar, con la transcripción en el `prompt`. Historial con
   «Escucha · unidad».
5. **Verificación en el móvil de Miguel Ángel.** Dos voces (o tonos)
   distinguibles; velocidad lenta; una respuesta bien y otra mal a
   propósito; transcripción al final; historial y «Mi progreso».

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| S3 | 2026-11-26 | Hito abierto por PCH. Anexo y hoja de ruta creados. |
| S3 | 2026-11-26 | Punto 1 hecho: ver «Comprobado en S3». |
| S3 | 2026-11-26 | Punto 2 hecho (`2eb4a86`): `FrenchVoice.speakDialogue` (dos voces francesas instaladas y sin red, o tono 0.75 para el segundo interlocutor si solo hay una; `QUEUE_ADD`), `isSpeaking` y `stop()`. |
| S3 | 2026-11-26 | Punto 3 hecho (`bf645e2`): `GeminiRepository.generateListening` (diálogo A/B de 6 a 10 réplicas y 4 preguntas en castellano con concepto) y `correctListeningAnswer` (juzga solo la comprensión, con el diálogo completo; respuesta esperada, explicación citando el francés, errores por concepto). |
| S3 | 2026-11-26 | Punto 4 hecho a falta de verificar (`86f0843`): `ui/listening/` con botón en el inicio; reproducción normal/lenta/parar sin texto; preguntas con corrección inmediata; transcripción al final; prompt con pregunta + transcripción para el historial («Escucha · unidad»); sesión `listening`; copia en Drive pendiente al terminar. `VoiceNotice` pasa a `ui/common/`. Inicio con desplazamiento vertical. |
| S3 | 2026-11-26 | **Desvío pedido por Miguel Ángel antes de las pruebas en el móvil:** logs de diagnóstico y subida a Drive, como en MiMoo (`com-actions-relay` §7). `data/log/AppLog` (un archivo por área en `filesDir/logs`: `crash_log`, `app_debug`, `gemini_debug`, `study_debug`, `speech_debug`, `drive_debug`; recorte a las últimas 400 líneas por encima de 200 KB; excepciones no capturadas al `crash_log`). Registro en cada llamada a Gemini (operación, duración, tamaños, código HTTP, cuerpo del error o respuesta ilegible, nunca la clave), Drive, planificador, temario, voz y grabación. Ajustes → «Logs de diagnóstico» → «Subir logs a Drive»: carpeta `miA2F - Intercambio Claude`, cada archivo sobrescrito en su sitio y `mia2f_logs_manifest.txt` el último. |
| S3 | 2026-11-27 | **Cambio pedido por Miguel Ángel tras usar la app:** la corrección era demasiado estricta (un punto o un guion que faltaban bloqueaban el test). `GeminiRepository.correctAnswer` (clases, tests y repasos) añade `MINOR_SLIPS_RULE`: puntuación, guiones, mayúsculas, espacios, comillas/apóstrofos tipográficos y letras cambiadas por despiste que no forman otra palabra ni cambian la gramática no son errores; se mencionan como «Detalle de escritura» en la explicación. Las tildes y la gramática siguen contando. |
