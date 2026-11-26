# PUNTO DE REANUDACIÓN — miA2F

**Última sesión:** S2 (2026-11-26), PCH a H03.
**Hito con la hoja de ruta activa:** ver `DOCS/ANNEX_ROUTER.md`

> El estado de los hitos vive **exclusivamente** en
> `DOCS/ANNEX_ROUTER.md`. Este archivo no lo declara ni lo duplica.

---

## Qué hacer en la siguiente sesión — EMPEZAR POR AQUÍ

1. **Empezar directamente, sin preguntar**, por el punto 1 de la hoja
   de ruta de `DOCS/ANNEX_H03.md` (verificación de la API de Gemini y
   lectura del método de `FrancaisA2`), y seguir en orden.

## Pendientes fuera del hito activo

- **H01, punto 8:** verificación en el móvil de la esposa de Miguel
  Ángel (instalar desde
  `https://github.com/MiguelaeTxio/miA2FReleases/releases/latest/download/miA2F.apk`,
  poner su nombre, escanear el QR que muestra el móvil de Miguel Ángel
  en Ajustes → «Pasar la clave a otro móvil» y probar la conexión).
  Se hace cuando ella esté disponible; Miguel Ángel avisa.
- **Android Developer Console:** el paquete `com.miguelaetxio.mia2f`
  quedó «In review» con la huella SHA-256 dada de alta. Cuando la
  consola muestre el fragmento de prueba de propiedad para la carpeta
  de assets, Miguel Ángel lo pega en el chat: se añade a
  `app/src/main/assets/` con el nombre que indique la consola, se
  publica una Release y él sube ese APK a la consola.

## Contexto técnico útil

- Gemini: modelo `gemini-3.5-flash` en `GeminiRepository` (único
  punto); proyecto `mia2f-515910` en prepago. La clave no admite
  restricción por app de Android (ver `MASTER_DOCUMENT.md` §2.3).
- `multiplatform-markdown-renderer` está fijado en 0.40.0: la 0.46.0
  exige compileSdk 37 y está compilada con Kotlin 2.4, que el
  procesador de Hilt aún no lee. No subirla sin subir antes Kotlin,
  KSP y Hilt.
- Temario: repositorio público `MiguelaeTxio/miCursos`, validar con
  `python3 tools/validate_course.py` antes de cada commit y subir
  `version` en `index.json` y en el `course.json` al cambiarlo.
- Aviso de temario: `CourseRepository.refresh()` devuelve
  `RefreshResult.Offline` ante `IOException`; `CourseViewModel` lo
  convierte en `RefreshNotice` (rojo solo si `isError`).
