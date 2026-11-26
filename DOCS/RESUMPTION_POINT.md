# PUNTO DE REANUDACIÓN — miA2F

**Última sesión cerrada:** S2 (2026-11-26).
**Hito con la hoja de ruta activa:** ver `DOCS/ANNEX_ROUTER.md`

> El estado de los hitos vive **exclusivamente** en
> `DOCS/ANNEX_ROUTER.md`. Este archivo no lo declara ni lo duplica.

---

## Qué hacer en la siguiente sesión — EMPEZAR POR AQUÍ

1. **Empezar directamente, sin preguntar**, por el punto 1 de la hoja
   de ruta de `DOCS/ANNEX_H04.md` (estado de cada concepto) y seguir
   en orden.
2. Si en cualquier momento Miguel Ángel trae resultados de la
   verificación de H03 (punto 8), atenderlos antes de seguir: todo
   fallo real se corrige en el momento, con evidencia.

## Pendientes fuera del hito activo

- **H03, punto 8:** verificación en el móvil de Miguel Ángel (clase
  completa, test con algún fallo y test aprobado, historial tras
  cerrar la app). Decidió hacerla al final, junto con la del punto 5
  de H04. La lista exacta de comprobaciones está en `ANNEX_H03.md`.
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

- Gemini: `generateContent` v1beta, modelo `gemini-3.5-flash` en
  `GeminiRepository` (único punto); JSON con
  `generationConfig.responseFormat.text`. Proyecto `mia2f-515910` en
  prepago. La clave no admite restricción por app de Android (ver
  `MASTER_DOCUMENT.md` §2.3). Documentación oficial ahora en
  `aistudio.google.com/docs` (ver `ANNEX_H03.md`, «Comprobado en S2»).
- Room 2.8.5, `study.db` versión 2. Todo cambio de esquema lleva
  migración; nunca `fallbackToDestructiveMigration`.
- `multiplatform-markdown-renderer` está fijado en 0.40.0: la 0.46.0
  exige compileSdk 37 y está compilada con Kotlin 2.4, que el
  procesador de Hilt aún no lee. No subirla sin subir antes Kotlin,
  KSP y Hilt.
- Temario: repositorio público `MiguelaeTxio/miCursos`, validar con
  `python3 tools/validate_course.py` antes de cada commit y subir
  `version` en `index.json` y en el `course.json` al cambiarlo.
- `api.github.com` sobre `miA2FReleases` no está accesible desde la
  sesión (403); el estado de compilación se lee con `gh run list`
  sobre `miA2F`.
