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
