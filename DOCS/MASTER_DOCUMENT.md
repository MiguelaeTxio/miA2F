# MIA2F — MASTER DOCUMENT

*Vive en `DOCS/MASTER_DOCUMENT.md` del propio repositorio
(`github.com/MiguelaeTxio/miA2F`) — flujo NewFlow Android. Editar y
commitear como cualquier otro archivo del proyecto, vía
`newflow-android-edit`. No es una skill. Puramente descriptivo: el
estado de los hitos vive solo en `DOCS/ANNEX_ROUTER.md`.*

---

## 1. Visión General del Proyecto

miA2F es una aplicación Android personal que lleva al móvil el curso
de francés A2 de refuerzo del proyecto `FrancaisA2`
(`github.com/MiguelaeTxio/FrancaisA2`), que hasta ahora solo existía
como sesiones de chat con Claude. Se instala en **dos dispositivos**,
igual que MiMoo: el de Miguel Ángel y el de su esposa. Cada
dispositivo es un alumno distinto con su propio progreso.

El curso sigue el mismo principio que `FrancaisA2`: refuerzo en
paralelo a la Escuela Oficial de Idiomas (libro *Prêt à porter*,
nivel A2), sin temario propio independiente. Lo que la app añade
frente al chat:

1. Ejercicios escritos con corrección de Claude vía API, siguiendo el
   método de corrección de `FrancaisA2` (frase por frase, explicación
   en castellano, sin silenciar errores).
2. Pronunciación sin pasar por Google Translate: la app reproduce la
   frase con voz francesa del propio dispositivo y transcribe lo que
   dice el alumno con el reconocimiento de voz del dispositivo en
   francés.
3. Escucha: diálogos cortos en francés reproducidos con la voz del
   dispositivo, con preguntas de comprensión corregidas por Claude.
4. Seguimiento de errores recurrentes por alumno, guardado en el
   propio dispositivo.

**Prioridad de construcción, de más a menos básico:**
1. Esqueleto, compilación en la nube, actualizaciones in-app, perfil
   de alumno, clave de API y primera llamada real a Claude (Hito 01).
2. Ejercicios escritos con corrección (Hito 02).
3. Pronunciación (Hito 03).
4. Escucha (Hito 04).
5. Progreso y errores recurrentes por alumno (Hito 05).

---

## 2. Arquitectura Técnica

### 2.1. Entorno de Desarrollo y Compilación

- **Lenguaje y UI:** Kotlin + Jetpack Compose (Material 3).
- **Inyección de dependencias:** Hilt.
- **Navegación:** Navigation Compose.
- **Persistencia local:** Room (SQLite) para progreso y errores;
  almacenamiento cifrado del dispositivo para la clave de API (§2.3).
- **Red:** Retrofit + OkHttp.
- **Versiones de toolchain:** las mismas que MiMoo en el momento de
  la inicialización (AGP 9.2.0, Kotlin 2.3.10, Gradle 9.4.1), para
  reutilizar una configuración ya probada en CI.
- **Sin Chaquopy ni Python embebido:** a diferencia de MiMoo, no hay
  nada que lo necesite.
- **Compilación:** exclusivamente en la nube vía GitHub Actions
  (`assembleDebug`, JDK 17). Tras cada push a `main`, el workflow
  publica el APK y su `manifest.json` como Release en el repositorio
  público dedicado `MiguelaeTxio/miA2FReleases`.

### 2.2. Por qué un repositorio de releases propio

MiMoo lee su actualización de
`AndroidReleases/releases/latest/download/manifest.json`. Si miA2F
publicase en `AndroidReleases`, su Release pasaría a ser la «latest»
de ese repositorio y MiMoo intentaría instalarse el APK de miA2F.
Por eso miA2F publica en su propio repositorio de releases, siguiendo
el mismo patrón que `miBTReleases` y `AperturasAjedrezReleases`.

### 2.3. Claude API

- **Endpoint:** Messages API (`POST /v1/messages`), llamado
  directamente desde el dispositivo por HTTPS.
- **Cuenta y facturación:** la API se factura aparte de la
  suscripción de claude.ai, a través de una cuenta de Claude Console.
  Los planes Max, Ultra y Team incluyen créditos mensuales de API si
  se enlaza una organización de Console al plan; fuera de esos
  créditos, el uso es de prepago en Console.
- **Clave de API:** **nunca** en el repositorio, ni como secret de
  CI inyectado en `BuildConfig`. El APK se publica en un repositorio
  público, y cualquier clave embebida en él sería extraíble por
  cualquiera que lo descargue. La clave se introduce una vez en la
  pantalla de Ajustes de cada dispositivo y se guarda cifrada en el
  propio dispositivo.
- **Modelo:** configurable en un único punto del código. El
  identificador concreto se verifica en línea contra la documentación
  oficial en el momento de implementarlo (§4.5), nunca de memoria.
- **Sin entrada de audio:** la Messages API acepta texto e imágenes,
  no audio. La pronunciación se evalúa sobre la transcripción que
  hace el reconocimiento de voz del dispositivo (§2.4), con la misma
  limitación honesta que en `FrancaisA2`: Claude no oye la
  pronunciación real.

### 2.4. Voz del dispositivo

- **Síntesis (TTS):** `android.speech.tts.TextToSpeech` con locale
  francés (`fr-FR`), para escuchar frases modelo y diálogos.
- **Reconocimiento (STT):** `android.speech.SpeechRecognizer` con
  idioma `fr-FR`, para transcribir lo que dice el alumno.
- Ambos dependen de los paquetes de voz instalados en cada
  dispositivo; la app debe detectar y avisar si falta el idioma
  francés.

### 2.5. Dos alumnos, dos dispositivos

Cada instalación guarda un único perfil de alumno (nombre elegido en
el primer arranque). El progreso de cada alumno vive en su propio
dispositivo. Sincronizar progreso entre dispositivos o con el
repositorio `FrancaisA2` queda fuera del alcance inicial.

### 2.6. Control de Versiones — NewFlow

`github.com/MiguelaeTxio/miA2F` es el único almacén de código y
documentación. Flujo: clon en el workspace del modelo → edición
directa → commit + push (`newflow-android-edit`). Cada push a `main`
es un intento de compilación completo.

---

## 3. Hoja de Ruta Estratégica

### Hito 01 — Esqueleto, CI/CD, actualizaciones in-app y conexión con Claude
(Ver `DOCS/ANNEX_H01.md`)
Proyecto Android compilando en la nube, Release publicada en
`miA2FReleases`, actualización in-app como en MiMoo, perfil de
alumno, clave de API guardada cifrada y una primera llamada real a
la Messages API verificada en los dos dispositivos.

### Hito 02 — Ejercicios escritos con corrección
Ejercicios del tramo activo del curso (empezando por *passé composé*
y conectores de tiempo), corregidos por Claude con el método de
`FrancaisA2`.

### Hito 03 — Pronunciación
Frase modelo con TTS francés, producción del alumno con STT francés,
comparación y comentario de Claude sobre la transcripción.

### Hito 04 — Escucha
Diálogos cortos reproducidos con TTS y preguntas de comprensión
corregidas por Claude.

### Hito 05 — Progreso y errores recurrentes
Registro por alumno de errores, vocabulario y nivel por destreza, con
el mismo criterio de «recurrente» que `FrancaisA2` (dos sesiones
distintas).

---

## 4. Directrices Técnicas Vinculantes

Estas directrices son de **OBLIGADO CUMPLIMIENTO** en todas las
sesiones del proyecto sin excepción.

### 4.1. Lectura de Entidades Antes de Codificar

**[PROHIBICIÓN ABSOLUTA]** Nunca inferir nombres de campos, funciones
de DAO/repositorio o tipos de datos desde memoria. Siempre se lee
primero el archivo real del clon antes de escribir código que
dependa de su contrato.

### 4.2. NavGraph y Pantallas No Implementadas

`NavGraph.kt` **nunca** referencia pantallas que todavía no existen.

### 4.3. Único Almacén — GitHub (NewFlow)

`github.com/MiguelaeTxio/miA2F` es el único almacén de código y
documentación. No existe ningún directorio de este proyecto en
PythonAnywhere.

### 4.4. Compilación Exclusivamente en la Nube

**QUEDA TERMINANTEMENTE PROHIBIDO** compilar localmente. Cada push a
`main` es un intento de compilación vía GitHub Actions.

### 4.5. Requisito SINE QUA NON

Antes de implementar cualquier código que involucre servicios
externos o APIs (Claude API, TextToSpeech, SpeechRecognizer, GitHub
Releases), el modelo **DEBE** verificar en línea la documentación
oficial vigente.

### 4.6. Secrets Nunca en Claro

Ningún secreto (clave de la Claude API, tokens de GitHub, keystore)
se escribe en código versionado, en `DOCS/`, en mensajes de commit ni
en `.git/config`. La clave de la Claude API **nunca** entra en el
APK: vive solo cifrada en cada dispositivo (§2.3).

### 4.7. Ningún Comentario de Cabecera con la Ruta del Archivo

**[PROHIBICIÓN ABSOLUTA]** Ningún archivo lleva como primera línea un
comentario con su propia ruta.

### 4.8. Método de corrección fiel a FrancaisA2

Los prompts que la app envía a Claude reproducen el método de
`FrancaisA2`: corrección honesta y directa en castellano, sin
silenciar errores, sin inventar reglas gramaticales, y sin evaluar la
pronunciación como si se hubiera oído.
