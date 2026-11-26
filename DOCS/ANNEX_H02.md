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

## HOJA DE RUTA

1. **Fuentes.** Localizar y leer en línea las fuentes oficiales del
   MCER para el A2 (descriptores generales y por destreza, volumen
   complementario) y el referencial de nivel A2 para el francés.
   Anotar en `miCursos/README.md` qué fuentes se usan y para qué.
2. **Formato del archivo de curso (v1).** Definir y documentar en
   `miCursos/README.md` el esquema JSON: un `index.json` raíz con la
   lista de cursos, y por curso un `course.json` con `id`, `language`,
   `level`, `version`, `title`, y `units[]`, cada una con `id`,
   `title`, `canDo[]` (objetivos «soy capaz de…» del MCER), `sources[]`
   y `concepts[]` (`id`, `type`: `grammar` | `vocabulary` |
   `communication` | `phonetics`, `title`, `explanation` en
   castellano, `examples[]` en francés). Incluir un validador
   (script Python en `miCursos/tools/`, con `py_compile` antes de
   subirlo) que compruebe el esquema y que los identificadores sean
   únicos.
3. **Temario de francés A2.** Redactar `miCursos/fr/A2/course.json`
   completo según las fuentes del punto 1, pasarlo por el validador y
   commitearlo en `miCursos`.
4. **Descarga y caché en la app.** Repositorio en la app que descarga
   `index.json` y el `course.json` del curso del alumno, lo guarda
   en el dispositivo, comprueba la versión al arrancar y funciona sin
   conexión con la copia guardada.
5. **Pantalla «Temario».** Desde el inicio, lista de unidades y, al
   tocar una, sus objetivos y conceptos (solo lectura).
6. **Verificación en el móvil.** Temario visible tras instalar la
   actualización, y visible también sin conexión.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| — | 2026-11-26 | Hito abierto por PCH. `miCursos` creado por Miguel Ángel. |
