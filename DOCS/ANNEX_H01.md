# MIA2F — ANEXO HITO 01
# Esqueleto, CI/CD, actualizaciones in-app y conexión con Gemini

*Vive en `DOCS/ANNEX_H01.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## OBJETIVO DEL HITO

Tener miA2F instalada y actualizándose sola en los dos dispositivos,
con perfil de alumno, clave de API guardada cifrada y una primera
llamada real a la API de Gemini que demuestre que la cadena
completa funciona antes de construir ningún ejercicio.

---

## REQUISITOS PREVIOS (los hace Miguel Ángel, fuera del repositorio)

1. Crear el repositorio **público** `MiguelaeTxio/miA2FReleases`
   (vacío, con un README para que tenga rama `main`).
2. Añadir en `miA2F` → Settings → Secrets and variables → Actions:
   - `RELEASES_REPO_LAST_TOKEN`: token con permiso de escritura de
     contenido sobre `miA2FReleases`.
   - `DEBUG_KEYSTORE_BASE64`: keystore de debug propia de miA2F
     (generada el 2026-11-26), para firmar siempre con la misma clave
     y que las actualizaciones no choquen. Huellas en
     `DOCS/SESSION_VARIABLES.md`.
3. Disponer de una clave de la API de Gemini (Google AI Studio,
   proyecto `mia2f-515910`, clave `miA2F Android`), restringida a la
   Gemini API; la restricción por app de Android no está disponible
   para estas claves, ver `MASTER_DOCUMENT.md` §2.3.
4. Permiso *Workflows* de la aplicación de Claude en GitHub sobre
   `miA2F`, para poder pushear `.github/workflows/`.
5. Registrar el paquete `com.miguelaetxio.mia2f` en la Android
   Developer Console (verificación de desarrollador de Android),
   aportando un APK firmado con la keystore de miA2F (cualquier
   Release de `miA2FReleases` sirve). Hoy no bloquea la instalación en
   España (la aplicación global está anunciada para 2027), pero se
   deja hecho igual que con MiMoo.

---

## HOJA DE RUTA

1. Esqueleto Gradle (wrapper, catálogo de versiones, módulo `app`),
   `MainActivity` Compose mínima y `.gitignore`.
2. Workflow `build-and-deploy.yml`: compilación, anotaciones de error,
   `manifest.json` y Release en `miA2FReleases`.
3. Actualización in-app (patrón de `AppUpdateRepository` de MiMoo,
   apuntando a `miA2FReleases`).
4. Primer arranque: perfil de alumno (nombre).
5. Ajustes: introducir la clave de la API de Gemini y guardarla cifrada.
6. Botón «Probar conexión» que envía una frase en francés a Gemini y
   muestra la corrección devuelta.
7. Pasar la clave al segundo dispositivo por código QR: el móvil que
   ya la tiene la muestra en Ajustes y el otro la escanea en el primer
   arranque, sin tener que entrar en AI Studio.
8. Verificación real en los dos dispositivos.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| — | 2026-11-26 | Proyecto inicializado (documentación base). Decidido solo Gemini. Puntos 1-5 hechos (v6): esqueleto, workflow, actualización in-app, perfil, clave cifrada en Ajustes; clave `miA2F Android` creada y guardada en el móvil de Miguel Ángel. Hechos: `miA2FReleases` creado, secrets `RELEASES_REPO_LAST_TOKEN` y `DEBUG_KEYSTORE_BASE64` creados. |
| — | 2026-11-26 (tarde) | Puntos 6 y 7 hechos: prueba de conexión real con Gemini (`gemini-3.5-flash`) con Markdown renderizado (v11, `multiplatform-markdown-renderer` 0.40.0 tras descartar la 0.46.0 por exigir compileSdk 37 y Kotlin 2.4), paso de la clave por QR (v12) y actualizaciones arriba del todo en Ajustes (v13). Verificado en el móvil de Miguel Ángel: actualización in-app, clave, corrección y QR. **Pendiente: punto 8 en el móvil de su esposa** (instalar, escanear el QR, probar conexión). |
