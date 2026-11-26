# MIA2F — ANEXO HITO 01
# Esqueleto, CI/CD, actualizaciones in-app y conexión con Claude

*Vive en `DOCS/ANNEX_H01.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## OBJETIVO DEL HITO

Tener miA2F instalada y actualizándose sola en los dos dispositivos,
con perfil de alumno, clave de API guardada cifrada y una primera
llamada real a la Claude Messages API que demuestre que la cadena
completa funciona antes de construir ningún ejercicio.

---

## REQUISITOS PREVIOS (los hace Miguel Ángel, fuera del repositorio)

1. Crear el repositorio **público** `MiguelaeTxio/miA2FReleases`
   (vacío, con un README para que tenga rama `main`).
2. Añadir en `miA2F` → Settings → Secrets and variables → Actions:
   - `RELEASES_REPO_LAST_TOKEN`: token con permiso de escritura de
     contenido sobre `miA2FReleases`.
   - `DEBUG_KEYSTORE_BASE64`: la misma keystore de debug que usa
     MiMoo, para firmar siempre con la misma clave y que las
     actualizaciones no choquen.
3. Disponer de una clave de la Claude API (Claude Console). La API se
   factura aparte de la suscripción de claude.ai; ver
   `MASTER_DOCUMENT.md` §2.3.
4. Permiso *Workflows* de la aplicación de Claude en GitHub sobre
   `miA2F`, para poder pushear `.github/workflows/`.

---

## HOJA DE RUTA

1. Esqueleto Gradle (wrapper, catálogo de versiones, módulo `app`),
   `MainActivity` Compose mínima y `.gitignore`.
2. Workflow `build-and-deploy.yml`: compilación, anotaciones de error,
   `manifest.json` y Release en `miA2FReleases`.
3. Actualización in-app (patrón de `AppUpdateRepository` de MiMoo,
   apuntando a `miA2FReleases`).
4. Primer arranque: perfil de alumno (nombre).
5. Ajustes: introducir la clave de la Claude API y guardarla cifrada.
6. Botón «Probar conexión» que envía una frase en francés a la
   Messages API y muestra la corrección devuelta.
7. Verificación real en los dos dispositivos.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| — | 2026-11-26 | Proyecto inicializado (documentación base). |
