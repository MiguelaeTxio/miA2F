# MIA2F — ANEXO HITO 04
# Evaluación continua y criterio de superación

*Vive en `DOCS/ANNEX_H04.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel («PCH a H04») en S2, con
H03 pausado y su punto 8 (verificación en el móvil) pendiente: Miguel
Ángel decidió probarlo todo al final.

---

## OBJETIVO DEL HITO

Que la app sepa, concepto a concepto, en qué punto está el alumno,
con el criterio de `FrancaisA2`: un error solo se da por superado
cuando deja de aparecer en al menos dos sesiones distintas, nunca por
una sola sesión sin errores. Que ese criterio gobierne qué se trabaja
en cada clase, que haya pruebas de repaso periódicas, y que el alumno
vea su progreso.

**Fuera de alcance:** copia en Drive (H05), pronunciación (H06) y
escucha (H07).

---

## CONTEXTO TÉCNICO

- **Registro actual (H03):** Room `study.db` v2 con `study_session`
  (`id`, `unit_id`, `started_at`, `ended_at`, `kind` =
  `lesson`/`test`), `exercise` (`session_id`, `position`,
  `concept_id`, `prompt`, `student_answer`, `correction`,
  `is_correct`) y `concept_error` (`session_id`, `exercise_id`,
  `concept_id`, `description`). DAO en `data/study/StudyDaos.kt`.
- **Planificador actual:** `StudyPlanner` toma como trabajado todo
  concepto acertado al menos una vez (`correctConceptIds`). H04 lo
  sustituye por el estado de cada concepto (punto 2).
- **Regla del test de unidad (acordada con Miguel Ángel en S2, se
  mantiene):** un test con todos los ejercicios bien da la unidad por
  superada y la clase siguiente pasa a la unidad siguiente.
- **Origen del criterio:** `FrancaisA2`,
  `DOCS/MAINS/FRANCAISA2_MASTER_DOCUMENT.md` §4.2.
- Toda tabla o columna nueva de Room va con su migración
  (`StudyDatabase`, versión siguiente), nunca con
  `fallbackToDestructiveMigration`: el progreso del alumno no se
  puede perder.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Estado de cada concepto.** Clase `ConceptProgressCalculator` (o
   nombre equivalente) que, a partir de Room, da a cada concepto del
   curso uno de estos estados:
   - **Sin trabajar:** nunca acertado.
   - **Acertado:** acertado al menos una vez y sin ningún error
     registrado.
   - **En observación:** tiene algún error y, después de la última
     sesión con error de ese concepto, hay menos de dos sesiones
     distintas en las que se acertó sin error.
   - **Superado:** tiene algún error, pero después de la última sesión
     con error hay al menos dos sesiones distintas en las que se
     acertó sin error.
   Además, todos los conceptos de una unidad con un test superado
   (todos sus ejercicios bien) cuentan como superados desde ese test,
   salvo que vuelvan a tener un error en una sesión posterior, que los
   devuelve a «en observación».
2. **El planificador usa el estado.** `StudyPlanner` elige la primera
   unidad que tenga conceptos sin trabajar o en observación; si no
   hay ninguna, la de más errores recientes, como hasta ahora. El
   resumen de progreso que recibe Gemini pasa a listar los conceptos
   por estado (en observación, con sus errores recientes; superados;
   acertados) y la instrucción de ejercicios pide priorizar los que
   están en observación.
3. **Prueba de repaso (evaluación continua).** Cada 3 clases
   terminadas desde el último repaso, el inicio muestra el botón «Hacer
   prueba de repaso» (destacado, sin bloquear «Empezar clase»). La
   prueba tiene un ejercicio por cada concepto en observación (hasta
   8), y si hay menos de 5, completa hasta 5 con conceptos acertados o
   superados de unidades ya trabajadas. Se guarda como sesión de tipo
   `review` (valor nuevo de `kind`, sin cambiar el esquema) y cuenta
   como una sesión más para el criterio del punto 1. Se reutilizan
   `LessonScreen`/`LessonViewModel` y la operación de Gemini por
   concepto (esquema con una propiedad obligatoria por concepto, como
   `generateTest`), generalizada para recibir la lista de conceptos.
4. **Pantalla «Mi progreso».** Botón en el inicio. Por unidad: número
   de conceptos en cada estado y la lista de conceptos con su estado;
   los que están en observación muestran sus errores recientes. Los
   tests y repasos se distinguen en el historial («Test» / «Repaso»).
5. **Verificación en el móvil de Miguel Ángel.** Junto con el punto 8
   de H03: un concepto fallado aparece en observación; tras acertarlo
   en dos sesiones distintas pasa a superado; tras 3 clases aparece
   la prueba de repaso y su resultado cambia los estados; «Mi
   progreso» refleja todo lo anterior.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| S2 | 2026-11-26 | Hito abierto por PCH. Anexo y hoja de ruta creados. Cierre de sesión (PCS). |
