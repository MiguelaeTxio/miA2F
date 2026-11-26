# MIA2F — ANEXO HITO 05
# Copia y restauración del progreso en Drive

*Vive en `DOCS/ANNEX_H05.md` — flujo NewFlow Android. Ver estado en
`DOCS/ANNEX_ROUTER.md`.*

---

## NOTA DE APERTURA (2026-11-26)

Abierto por PCH explícito de Miguel Ángel («PCH a H05») en S3, con H04
pausado y su punto 5 (verificación en el móvil) pendiente: Miguel
Ángel prueba todo al final, junto con el punto 8 de H03.

---

## OBJETIVO DEL HITO

Que el progreso de cada alumno no dependa de su teléfono: una copia
en el Drive de la cuenta de Google de ese mismo móvil (la de Miguel
Ángel en el suyo, la de su esposa en el de ella, nunca cruzadas), que
se actualiza sola, y una restauración que deja el progreso exactamente
como estaba tras perder, cambiar o reinstalar el teléfono. La esposa de
Miguel Ángel no es técnica: copiar y restaurar no pueden exigirle más
que elegir su cuenta y aceptar el permiso de Google.

**Fuera de alcance:** sincronizar entre los dos alumnos (no se hace
nunca, `MASTER_DOCUMENT.md` §2.5), pronunciación (H06) y escucha
(H07).

---

## DECISIONES DE PARTIDA (tomadas al abrir el hito, revisables)

- **Mismo patrón que MiMoo H06**, ya verificado en un móvil real:
  autorización con `Identity.getAuthorizationClient()`
  (`play-services-auth`), scope **`drive.file`** (la app solo ve los
  archivos que ella crea, nunca el resto del Drive) y Drive REST v3 por
  Retrofit, con el token como cabecera por llamada. En MiMoo el Client
  ID **no se usa en el código**: no hace falta secret de Actions ni
  `BuildConfig` para él.
- **Qué se copia:** el nombre del alumno y todo el registro de Room
  (`study_session`, `exercise`, `concept_error`), en un único JSON con
  versión de formato. **No** se copia la clave de Gemini (secreto,
  `MASTER_DOCUMENT.md` §4.6): en un móvil nuevo se vuelve a pasar por
  QR desde Ajustes del otro móvil. **No** se copia el temario: se
  vuelve a descargar de `miCursos`.
- **Un solo archivo por alumno**, `mia2f_progreso.json`, que se
  sobrescribe en cada copia (no se acumulan copias).
- **Cuándo se copia:** sola al terminar cada clase, test o repaso, y a
  mano con un botón en Ajustes.
- **Restaurar:** botón en Ajustes y oferta en el primer arranque («¿Ya
  usabas miA2F en otro móvil?»). Siempre con confirmación, porque
  sustituye el progreso local.

---

## CONTEXTO TÉCNICO

- **Proyecto de Google Cloud:** `mia2f-515910` (el de Gemini, en
  prepago). La Drive API no tiene coste por uso.
- **Firma de la app:** keystore de debug propia de miA2F; huellas
  SHA-1 y SHA-256 en `DOCS/SESSION_VARIABLES.md`. El cliente OAuth
  Android se crea con el paquete `com.miguelaetxio.mia2f` y esa SHA-1.
- **Lecciones de MiMoo H06** (`MiguelaeTxio/MiMoo`,
  `DOCS/ANNEX_H06.md`, «INVESTIGACIÓN H06 PASO 6»):
  - Hace falta, además del cliente Android, un cliente OAuth de tipo
    **Web application** en el mismo proyecto, aunque la app no lo use
    en el código.
  - Con la pantalla de consentimiento en modo **Testing**, cada cuenta
    que vaya a usar la app tiene que estar dada de alta como **test
    user**: aquí, la de Miguel Ángel y la de su esposa.
  - Un proyecto de Google Cloud puede quedar con el registro OAuth
    roto sin ningún síntoma en la consola: si con todo bien
    configurado la autorización falla siempre igual, se prueba en un
    proyecto nuevo antes de seguir tocando código.
- **Código de referencia en MiMoo** (leer el archivo real antes de
  reutilizarlo, §4.1): `data/backup/DriveAuthorizationHelper.kt`,
  `data/remote/DriveApiService.kt`, `data/remote/dto/DriveDtos.kt`.
- **Room:** `study.db` versión 2 (`StudyDatabase`). La restauración
  escribe en una transacción y conserva los `id`: el criterio de
  superación de H04 ordena las clases por `id`.

### Comprobado en S3 (documentación oficial, 2026-11-26)

- **Autorización** (`developer.android.com/identity/authorization`):
  `Identity.getAuthorizationClient(…).authorize(AuthorizationRequest
  .builder().setRequestedScopes(…).build())`; si `hasResolution()`, se
  lanza `pendingIntent.intentSender` con `StartIntentSenderForResult` y
  el resultado se lee con `getAuthorizationResultFromIntent`; el token
  sale de `accessToken`. El token dura unos 60 minutos y está en caché;
  volver a llamar a `authorize()` devuelve uno nuevo sin interacción
  mientras el permiso siga concedido. Si un token falla, se quita con
  `clearToken(ClearTokenRequest…)` y se vuelve a pedir. La página usa
  `play-services-auth:22.0.0`; las notas de versión de Google Play
  services dicen que la 22.0.0 retira las API de Google Sign-in, pero
  no la de autorización.
- **Drive v3:** crear con `POST
  https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`
  (`multipart/related`: metadatos JSON + contenido); sobrescribir con
  `PATCH https://www.googleapis.com/upload/drive/v3/files/{fileId}
  ?uploadType=media`; buscar con `q = name = '…' and trashed = false`;
  descargar con `alt=media`. `files.update` admite el scope
  `drive.file`.
- **Modo Testing (Google Cloud, «Manage App Audience»):** las
  autorizaciones de un test user **caducan a los siete días** del
  consentimiento. En la práctica: una vez por semana, la copia
  automática encontrará que hace falta volver a aceptar el permiso.
  **Decisión de Miguel Ángel (S3): se queda en Testing, como MiMoo.**
  Publicar («In production») exige URL de página principal y de
  política de privacidad en Branding (aviso del botón «Publish app»).
  Y la evidencia real de MiMoo, también en Testing, es que en meses de
  uso ni Miguel Ángel ni su esposa han tenido que volver a aceptar el
  permiso: en Android la autorización va por Google Play services con
  la cuenta del móvil. Si alguna vez Google lo vuelve a pedir, la app
  lanza la pantalla de permiso como `AutoSyncViewModel.startAutoSync`
  de MiMoo y continúa la copia.

---

## HOJA DE RUTA PARA LA SIGUIENTE SESIÓN

1. **Google Cloud, guiado paso a paso con Miguel Ángel** en el proyecto
   `mia2f-515910`: habilitar la Google Drive API; en Google Auth
   Platform, Audience en modo Testing con su cuenta y la de su esposa
   como test users, y el scope `drive.file` en Data Access; crear el
   cliente OAuth **Android** (paquete y SHA-1 de
   `SESSION_VARIABLES.md`) y el cliente **Web application**. Anotar en
   `SESSION_VARIABLES.md` los nombres de los clientes (los valores no
   hacen falta en el repositorio).
2. **Documentación oficial vigente (§4.5)**, antes de escribir código:
   `AuthorizationClient` y última versión estable de
   `play-services-auth`; Drive v3 `files.create` y `files.update` con
   subida `multipart` y descarga `alt=media`; y qué pasa con la
   autorización en modo Testing pasados unos días (si Google vuelve a
   pedir el permiso, la copia automática tiene que pedirlo de forma
   legible, nunca fallar en silencio).
3. **Formato de copia.** `ProgressBackup` (versión de formato, fecha,
   nombre del alumno, versión de `study.db`, y las filas de las tres
   tablas con sus `id`). Exportar desde Room y restaurar en una
   transacción que vacía las tablas y las rellena; una copia con
   versión de formato o de base de datos más nueva que la app se
   rechaza con un mensaje claro.
4. **Capa de Drive.** `DriveAuthorizationHelper` y `DriveApiService`
   siguiendo MiMoo, y `ProgressBackupRepository`: buscar
   `mia2f_progreso.json` creado por la app; crearlo si no existe o
   sobrescribirlo si existe; descargar el último para restaurar.
5. **Copia automática y manual.** Al terminar una clase, test o
   repaso, copia en segundo plano sin bloquear al alumno. Si falta el
   permiso o no hay conexión, se queda pendiente y el inicio lo dice
   en una línea. En Ajustes: cuenta usada, fecha de la última copia
   buena y botón «Copiar ahora».
6. **Restaurar.** En Ajustes, «Restaurar desde Drive» con
   confirmación (fecha de la copia, número de clases que trae). En el
   primer arranque, tras poner el nombre, ofrecer restaurar si hay
   copia en la cuenta elegida.
7. **Verificación en los dos móviles.** Copia hecha sola tras una
   clase; borrar los datos de la app (o reinstalarla), restaurar y
   comprobar que «Mi progreso» y el historial son idénticos a los de
   antes. En el de su esposa, comprobar que solo elige cuenta y acepta
   el permiso, y que su copia no se mezcla con la de Miguel Ángel.

---

## REGISTRO DE SESIONES

| Sesión | Fecha | Trabajo realizado |
|---|---|---|
| S3 | 2026-11-26 | Hito abierto por PCH. Anexo y hoja de ruta creados a partir del patrón de Drive ya verificado en MiMoo H06. |
| S3 | 2026-11-26 | Branding completado (nombre, correo de asistencia y de contacto). Intento de publicar descartado: pide URL de página principal y de política de privacidad. Se queda en Testing (ver «Comprobado en S3»). |
| S3 | 2026-11-26 | Punto 1 hecho con Miguel Ángel en la consola: Google Auth Platform creado en `mia2f-515910` (External, Testing), 2 test users, scope `drive.file`, clientes OAuth `miA2F Android` y `miA2F Web`. Anotado en `SESSION_VARIABLES.md`. La habilitación de la Drive API (Biblioteca) no se confirmó con captura: si la primera llamada a Drive devuelve 403 «API not enabled», es eso. |
