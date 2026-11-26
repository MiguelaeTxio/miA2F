# MIA2F — ENRUTADOR DE ANEXOS

*Vive en `DOCS/ANNEX_ROUTER.md`. **Único archivo del repositorio con
potestad para decir qué hito está EN PROGRESO.** Ningún otro archivo
— `MASTER_DOCUMENT.md`, ningún `ANNEX_H0X.md`, `RESUMPTION_POINT.md` —
contiene esa información.*

---

## PRINCIPIO FUNDAMENTAL

Solo existen **dos** estados posibles para un hito: **EN PROGRESO** y
**PAUSADO**. Nunca «completado».

Debe haber **siempre exactamente un** hito EN PROGRESO. Todos los
demás están PAUSADOS. Solo se crea el anexo de un hito cuando se abre
por primera vez.

---

## TABLA DE ESTADO

| Hito | Anexo | Estado |
|---|---|---|
| H01 | `DOCS/ANNEX_H01.md` | PAUSADO |
| H02 | `DOCS/ANNEX_H02.md` | ← **EN PROGRESO** |

---

## HISTORIAL DE CAMBIOS DE HITO

- **2026-11-26** — Inicialización del proyecto. H01 abierto EN
  PROGRESO.
- **2026-11-26** — PCH explícito de Miguel Ángel («PCH a H02»): **H01
  pasa a PAUSADO** con el punto 8 de su hoja de ruta pendiente
  (verificación en el móvil de su esposa, que se hará cuando ella
  esté disponible). **H02 (Repositorio de cursos y temario A2 según
  el MCER) abierto EN PROGRESO**, tras rediseñar la app como curso
  completo con Gemini de profesora y progreso copiado en Drive.
