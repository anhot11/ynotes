# 🤖 Guía de Publicación en F-Droid para yNotes (`app.uamo.ynotes`)

## ¿Por qué F-Droid requiere esta estructura?
F-Droid es una tienda de software libre que **no acepta binarios precompilados enviados por usuarios**. F-Droid compila el código fuente de tu aplicación desde cero en sus propios servidores aislados para garantizar que el APK sea 100% libre de rastreadores y código propietario.

---

## 🛠️ Archivos creados e integrados en el repositorio:
1. **Receta de compilación F-Droid**: [`metadata/app.uamo.ynotes.yml`](file:///C:/Users/Usuario/Documents/yNotes/metadata/app.uamo.ynotes.yml)
2. **Estructura Metadata Fastlane**: [`fastlane/metadata/android/en-US/`](file:///C:/Users/Usuario/Documents/yNotes/fastlane/metadata/android/en-US/)
3. **Licencia de Código Abierto (MIT)**: [`LICENSE`](file:///C:/Users/Usuario/Documents/yNotes/LICENSE)

---

## 🚀 Pasos sencillos para solicitar la inclusión oficial en F-Droid:

### Opción A: Crear una solicitud de inclusión (RFP / Issue) en GitLab de F-Droid (Recomendada)
1. Entra a las solicitudes de F-Droid: **[F-Droid Request For Inclusion (RFP)](https://gitlab.com/fdroid/rfp/-/issues)**
2. Haz clic en **"New Issue"** y selecciona la plantilla **"Inclusion Request"**.
3. Completa los datos:
   - **App Name**: `yNotes`
   - **Application ID**: `app.uamo.ynotes`
   - **Source Code**: `https://github.com/xxnonxxp11/ynotes`
   - **License**: `MIT`
4. Pega el contenido del archivo [`metadata/app.uamo.ynotes.yml`](file:///C:/Users/Usuario/Documents/yNotes/metadata/app.uamo.ynotes.yml).
5. ¡Listo! El bot de F-Droid compilará la app automáticamente desde el tag `v1.0.1` de tu repositorio de GitHub.
