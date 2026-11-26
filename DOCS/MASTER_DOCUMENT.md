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

**Rediseño del 2026-11-26 (acordado con Miguel Ángel antes del
PCH a H02):** la app es un **curso completo de francés A2 según el
MCER**, no un refuerzo ligado al libro de la EOI. Tres piezas:

1. **El curso como datos.** El temario (unidades, objetivos y
   conceptos de gramática, vocabulario, comunicación y fonética, cada
   uno con identificador estable) vive en un archivo estructurado en
   el repositorio público de cursos `MiguelaeTxio/miCursos`, redactado
   una vez y revisado contra los descriptores oficiales del MCER para
   el A2 — nunca inventado por Gemini sobre la marcha. La app lo
   descarga y lo guarda; el mismo repositorio deja la puerta abierta a
   otros niveles e idiomas.
2. **Gemini como profesora.** En cada sesión recibe la parte del
   temario que toca y un resumen del progreso del alumno; pone
   ejercicios, los corrige con el método de `FrancaisA2` (castellano,
   honesto, sin silenciar errores), hace pruebas de evaluación
   continua y devuelve qué conceptos se trabajaron y qué errores
   salieron. La app registra cada sesión hasta terminar el curso.
3. **Progreso a salvo.** El progreso de cada alumno se guarda en su
   móvil y se copia en **el Drive de su propia cuenta de Google**,
   como en MiMoo, para no perderlo si se pierde el teléfono.

Además: pronunciación evaluada sobre la **grabación real** de la voz
del alumno, y escucha con diálogos reproducidos por la voz francesa
del dispositivo.

**Prioridad de construcción:** ver §3 (H01 → H07).

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
dispositivo y se copia en el Drive de la cuenta de Google de ese
mismo dispositivo (H05). No se sincroniza entre los dos alumnos.

### 2.7. Repositorio de cursos `miCursos`

Repositorio **público** `MiguelaeTxio/miCursos`, solo de contenido
(sin código de la app, sin datos personales). Un curso por carpeta
(`fr/A2/` el primero). La app descarga el curso, lo guarda en caché y
comprueba su versión; corregir o ampliar un temario no exige sacar
una versión nueva de la app. El formato del archivo se fija en H02.

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

### Hito 02 — Repositorio de cursos y temario A2 según el MCER
(Ver `DOCS/ANNEX_H02.md`)
Formato del archivo de curso, temario completo de francés A2 en
`miCursos` contrastado con las fuentes oficiales del MCER, y descarga,
caché y consulta del temario en la app.

### Hito 03 — La profesora: sesiones con ejercicios y corrección
Sesiones guiadas por Gemini según el temario y el progreso del
alumno: ejercicios escritos, corrección con el método de
`FrancaisA2` y registro de cada sesión.

### Hito 04 — Evaluación continua y criterio de superación
Pruebas de evaluación continua y criterio para dar un concepto por
superado (el error deja de aparecer en al menos dos sesiones
distintas, como en `FrancaisA2`).

### Hito 05 — Copia y restauración del progreso en Drive
Copia del progreso en el Drive de la cuenta de Google de cada
dispositivo y restauración tras perder o cambiar de teléfono.

### Hito 06 — Pronunciación
Frase modelo con TTS francés, grabación del alumno y comentario de
Gemini sobre el audio real.

### Hito 07 — Escucha
Diálogos cortos reproducidos con TTS y preguntas de comprensión
corregidas por Gemini.

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
