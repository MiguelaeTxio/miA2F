# MIA2F — ANEXO HITO 02
# Repositorio de cursos y temario A2 según el MCER

*Vive en `DOCS/ANNEX_H02.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel tras rediseñar la app como
curso completo (ver `MASTER_DOCUMENT.md` §1 y §2.7). Decisiones suyas
en la misma sesión:

- Temario ceñido al **estándar del MCER** para el nivel A2, no al
  orden del libro *Prêt à porter*.
- Copia del progreso en el Drive de **cada cuenta por separado** (se
  implementa en H05, no aquí).
- Repositorio de cursos: **`MiguelaeTxio/miCursos`**, público, creado
  por Miguel Ángel el 2026-11-26 con un README inicial.

---

## OBJETIVO DEL HITO

Que exista un temario de francés A2 completo, estructurado y
verificable en `miCursos`, y que la app lo descargue, lo guarde y lo
muestre. Este hito **no** incluye todavía sesiones con Gemini (H03):
solo el curso como datos y su llegada al móvil.

---

## CONTEXTO TÉCNICO

- El progreso futuro (H03–H05) apuntará a **identificadores de
  concepto estables** (p. ej. `fr-a2.grammaire.passe-compose-etre`).
  Una vez publicado un identificador, **no se renombra ni se
  reutiliza**: si un concepto cambia de sentido, se crea otro.
- Fuentes de autoridad para el temario, a consultar en línea al
  redactarlo (directriz §4.5 del documento maestro y §4.4 de
  `FrancaisA2`, nunca de memoria): descriptores del nivel A2 del MCER
  del Consejo de Europa (incluido el volumen complementario) y el
  referencial de nivel A2 para el francés. Cada unidad del temario
  cita la fuente de la que sale.
- Descarga en la app: el repositorio es público, así que el archivo
  se lee sin autenticación desde GitHub (misma idea que el
  `manifest.json` de `miA2FReleases`). El método exacto (URL directa
  de la rama o Release) se decide en el punto 4 tras verificarlo en
  línea.
- Acceso del modelo a `miCursos`: enlazar el repositorio a la sesión
  con `add_repo` (acceso `push`) antes del punto 2.

---

## COMPLETADAS EN S1 (2026-11-26)

Los seis puntos de la hoja de ruta original del hito, todos hechos y
verificados (detalle y commits en el registro de sesiones):

1. Fuentes oficiales del MCER documentadas en `miCursos/README.md`.
2. Formato v1 de `index.json` y `course.json` y validador
   `miCursos/tools/validate_course.py`.
3. Temario de francés A2 v1: 12 unidades, 57 conceptos, 0 errores.
4. Descarga y caché del temario en la app (`CourseRepository`).
5. Pantalla «Temario» con unidades, objetivos y conceptos.
6. Verificado en el móvil de Miguel Ángel, con y sin conexión.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Mensaje de «sin conexión» legible** (petición de Miguel Ángel al
   cerrar S1). Hoy, sin Internet, el inicio muestra en rojo el texto
   técnico de la excepción («No se pudo comprobar el temario: Unable
   to resolve host…»). Sustituirlo así:
   - Si el fallo es de red (`java.io.IOException`, que incluye
     `UnknownHostException`) y hay curso guardado: «Sin conexión:
     usando el temario guardado (versión N).», en el color normal del
     texto, no en rojo.
   - Si es de red y no hay curso guardado: «Sin conexión: el temario
     se descargará en cuanto haya Internet.»
   - Cualquier otro fallo (respuesta inesperada de GitHub, JSON
     inválido): se mantiene en rojo con su mensaje.
   - Criterio de cierre: verificado en el móvil en modo avión (primer
     caso) y tras volver la conexión (sin aviso).

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| — | 2026-11-26 | Hito abierto por PCH. `miCursos` creado por Miguel Ángel. |
| — | 2026-11-26 (tarde) | Puntos 1-3 hechos en `miCursos`: fuentes del MCER en el README (`c1fe43f`), formato v1 y `tools/validate_course.py` (`eb0fae9`), temario de francés A2 v1 con 12 unidades y 57 conceptos, validado con 0 errores (`38cd920`). Comprobado que `raw.githubusercontent.com/MiguelaeTxio/miCursos/main/index.json` responde 200 sin autenticación. |
| — | 2026-11-26 (tarde) | Puntos 4-6 hechos: descarga y caché del temario desde `miCursos` (v16, `c8a9950`) y pantalla Temario (v17, `00e1b01`). Verificado por Miguel Ángel en su móvil: el inicio muestra «Francés A2 (MCER) — versión 1 del temario, 12 unidades», la unidad 3 se ve completa, y en modo avión el temario sigue disponible desde la copia guardada (con el aviso de que no se pudo comprobar), y al volver la conexión el aviso desaparece. Hoja de ruta de H02 completa. |
| S1 | 2026-11-26 | Cierre de sesión: registro de lo completado y hoja de ruta nueva (mensaje de «sin conexión» legible). |
