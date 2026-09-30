# Build BIOPHYSX APK

### Android Studio
1. Open the `BIOPHYSX-Android` folder.
2. Let Android Studio install/sync the Android Gradle Plugin requirements.
3. Select **Build > Build APK(s)**.
4. The debug APK will appear under `app/build/outputs/apk/debug/`.

### GitHub
Push the project to GitHub. The included workflow `.github/workflows/android.yml` runs automatically on `main`/`master`, and it can also be started manually from the Actions tab.

The workflow uploads `app-debug.apk` as an artifact named **BIOPHYSX-debug-apk**.
