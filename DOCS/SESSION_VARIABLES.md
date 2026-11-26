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
- `DEBUG_KEYSTORE_BASE64` — la misma keystore de debug que MiMoo.

## Clave de la Claude API

**No es un secret de GitHub Actions y nunca entra en el APK.** Se
introduce en Ajustes de cada dispositivo y se guarda cifrada allí. Ver
`MASTER_DOCUMENT.md` §2.3 y §4.6.

## Notas

- Ningún token de GitHub vive en este archivo ni en ningún otro
  archivo del repositorio (`newflow-android-token`).
