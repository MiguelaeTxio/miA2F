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

1. Ejercicios escritos corregidos por Gemini vía API, siguiendo el
   método de corrección de `FrancaisA2` (frase por frase, explicación
   en castellano, sin silenciar errores).
2. Pronunciación evaluada sobre la **grabación real** de la voz del
   alumno: la app reproduce la frase modelo con voz francesa del
   dispositivo, graba al alumno y envía el audio a Gemini.
3. Escucha: diálogos cortos en francés reproducidos con la voz del
   dispositivo, con preguntas de comprensión corregidas por Gemini.
4. Seguimiento de errores recurrentes por alumno, guardado en el
   propio dispositivo.

**Prioridad de construcción, de más a menos básico:**
1. Esqueleto, compilación en la nube, actualizaciones in-app, perfil
   de alumno, clave de API y primera llamada real a Gemini (Hito 01).
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

### 2.3. Gemini API (decisión 2026-11-26: solo Gemini)

Decisión explícita de Miguel Ángel: **solo Gemini**, sin Claude API.
Motivo principal: la API de Gemini acepta audio como entrada, así que
la pronunciación se evalúa sobre la grabación real y no solo sobre
una transcripción. Además tiene nivel gratuito.

- **Llamada:** Gemini Developer API, directamente desde el
  dispositivo por HTTPS.
- **Clave de API:** **nunca** en el repositorio, ni como secret de
  CI inyectado en `BuildConfig`. El APK se publica en un repositorio
  público, y cualquier clave embebida en él sería extraíble por
  cualquiera que lo descargue. La clave se introduce una vez en la
  pantalla de Ajustes de cada dispositivo y se guarda cifrada en el
  propio dispositivo.
- **Restricción de la clave (corregido 2026-11-26):** las claves de
  la API de Gemini creadas en AI Studio van ligadas a una cuenta de
  servicio, y Google Cloud **no permite** restringir esas claves por
  app de Android (comprobado en la propia consola). La clave queda
  restringida solo a la **Gemini API**, sin restricción de
  aplicación. Lo compensan tres cosas: la clave no va en el APK, se
  guarda cifrada en el Android Keystore de cada dispositivo, y el
  proyecto `mia2f-515910` funciona en **prepago**, así que el gasto
  máximo ante una filtración es el saldo cargado. Ante cualquier
  sospecha: revocar la clave en AI Studio y crear otra.
- **Paso de la clave al segundo dispositivo:** por código QR mostrado
  en Ajustes del dispositivo que ya la tiene y escaneado por el otro
  (petición de Miguel Ángel: su esposa no debe tener que entrar en AI
  Studio). La clave no pasa por ningún servicio de mensajería.
- **Nivel gratuito y privacidad:** en el nivel gratuito Google puede
  usar las peticiones (textos y grabaciones de voz) para mejorar sus
  productos; en el de pago, no. La elección de nivel es de Miguel
  Ángel y no cambia el código.
- **Modelo:** configurable en un único punto del código. El
  identificador concreto se verifica en línea contra la documentación
  oficial en el momento de implementarlo (§4.5), nunca de memoria.

### 2.4. Voz del dispositivo

- **Síntesis (TTS):** `android.speech.tts.TextToSpeech` con locale
  francés (`fr-FR`), para escuchar frases modelo y diálogos.
- **Grabación:** captura de la voz del alumno en un formato de audio
  admitido por Gemini, enviada como entrada de audio.
- La síntesis depende de los paquetes de voz instalados en cada
  dispositivo; la app debe detectar y avisar si falta el francés.

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

### Hito 01 — Esqueleto, CI/CD, actualizaciones in-app y conexión con Gemini
(Ver `DOCS/ANNEX_H01.md`)
Proyecto Android compilando en la nube, Release publicada en
`miA2FReleases`, actualización in-app como en MiMoo, perfil de
alumno, clave de API guardada cifrada y una primera llamada real a
Gemini verificada en los dos dispositivos.

### Hito 02 — Ejercicios escritos con corrección
Ejercicios del tramo activo del curso (empezando por *passé composé*
y conectores de tiempo), corregidos por Gemini con el método de
`FrancaisA2`.

### Hito 03 — Pronunciación
Frase modelo con TTS francés, grabación del alumno y comentario de
Gemini sobre el audio real.

### Hito 04 — Escucha
Diálogos cortos reproducidos con TTS y preguntas de comprensión
corregidas por Gemini.

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
externos o APIs (Gemini API, TextToSpeech, grabación de audio, GitHub
Releases), el modelo **DEBE** verificar en línea la documentación
oficial vigente.

### 4.6. Secrets Nunca en Claro

Ningún secreto (clave de la API de Gemini, tokens de GitHub,
keystore) se escribe en código versionado, en `DOCS/`, en mensajes
de commit ni en `.git/config`. La clave de Gemini **nunca** entra en
el APK: vive solo cifrada en cada dispositivo (§2.3).

### 4.7. Ningún Comentario de Cabecera con la Ruta del Archivo

**[PROHIBICIÓN ABSOLUTA]** Ningún archivo lleva como primera línea un
comentario con su propia ruta.

### 4.8. Método de corrección fiel a FrancaisA2

Los prompts que la app envía a Gemini reproducen el método de
`FrancaisA2`: corrección honesta y directa en castellano, sin
silenciar errores y sin inventar reglas gramaticales.
