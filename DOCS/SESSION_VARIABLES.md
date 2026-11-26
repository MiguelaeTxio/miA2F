# MIA2F — VARIABLES DE SESIÓN (NewFlow Android)

*Vive en `DOCS/SESSION_VARIABLES.md` — editable directamente en
GitHub o vía `newflow-android-edit`. No es una skill.*

| Variable | Valor |
|---|---|
| `ANDROID_APP_NAME` | miA2F |
| `ANDROID_PACKAGE` | `com.miguelaetxio.mia2f` |
| `ANDROID_GITHUB_REPO` | `https://github.com/MiguelaeTxio/miA2F.git` |
| `ANDROID_GITHUB_OWNER` | MiguelaeTxio |
| `ANDROID_GITHUB_BRANCH` | main |
| `LOCAL_CLONE_PATH` (workspace del modelo) | `/home/claude/mia2f` |
| `MASTER_DOCUMENT_PATH` | `DOCS/MASTER_DOCUMENT.md` |
| `ROUTER_PATH` (única fuente de verdad del hito EN PROGRESO) | `DOCS/ANNEX_ROUTER.md` |
| `ANNEX_PATH_PATTERN` | `DOCS/ANNEX_H{NN}.md` |
| `RESUMPTION_POINT_PATH` | `DOCS/RESUMPTION_POINT.md` |
| `RELEASES_REPO` (público, Releases + `manifest.json`) | `MiguelaeTxio/miA2FReleases` |
| `COURSE_SOURCE_REPO` (curso de chat del que sale el contenido) | `MiguelaeTxio/FrancaisA2` |

## Secrets de GitHub Actions (no vive el valor aquí, solo el nombre)

- `RELEASES_REPO_LAST_TOKEN` — escritura sobre `miA2FReleases`.
- `DEBUG_KEYSTORE_BASE64` — keystore de debug propia de miA2F
  (generada el 2026-11-26; alias `androiddebugkey`, contraseñas por
  defecto de debug de Android). No es la de MiMoo: al ser otro
  paquete, no necesita compartir firma. Huellas públicas, para
  verificar la keystore restaurada en el workflow y para restringir
  claves de API de Google al paquete:
  - SHA-1: `2E:D2:47:BF:4F:19:50:E4:97:03:77:FC:A8:8C:FE:E1:C2:F0:B2:A7`
  - SHA-256: `8A:14:BE:97:22:F4:17:61:39:EC:08:ED:FA:30:81:DF:84:10:28:2A:2E:9E:1C:6A:52:8D:EB:24:9A:78:6F:1E`

## Clave de la Claude API

**No es un secret de GitHub Actions y nunca entra en el APK.** Se
introduce en Ajustes de cada dispositivo y se guarda cifrada allí. Ver
`MASTER_DOCUMENT.md` §2.3 y §4.6.

## Notas

- Ningún token de GitHub vive en este archivo ni en ningún otro
  archivo del repositorio (`newflow-android-token`).
