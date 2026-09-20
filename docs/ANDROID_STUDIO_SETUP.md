# Abrir y ejecutar en Android Studio

1. Descomprime `SIAA_Android_Full_v0_7.zip`.
2. Abre la carpeta raíz `SIAA_Android_Full_v0_7` como proyecto.
3. Usa JDK 17 para Gradle.
4. Instala Android SDK Platform 37 desde SDK Manager.
5. Si falta `gradle/wrapper/gradle-wrapper.jar`, ejecuta `scripts/bootstrap_gradle.sh --version` (o el `.ps1` en Windows) una vez con Internet.
6. Sincroniza Gradle.
7. Ejecuta `:app:assembleDebug` o pulsa Run sobre un dispositivo Android físico.
8. Conecta tus audífonos Bluetooth y entra en **Audífonos > Escuchar controles** para comprobar qué comandos expone su firmware.
9. Guarda el perfil y prueba primero una sesión de 10 actividades con la pantalla encendida; luego repite con la pantalla bloqueada.

## QA recomendado antes de tocar hardware

```bash
python scripts/validate_project.py
python scripts/profile_audit.py
./scripts/smoke_pure_kotlin.sh
./scripts/smoke_content.sh
./scripts/smoke_runtime.sh
python scripts/verify_release.py
```

## Primer build esperado

```bash
./gradlew test
./gradlew :app:assembleDebug
```

APK esperado:
`app/build/outputs/apk/debug/app-debug.apk`

## Nota sobre audífonos
La app no recibe universalmente el número físico de taps. El firmware del audífono traduce gestos a comandos multimedia. SIAA consume los comandos que Android/MediaSession recibe y los interpreta según el estado pedagógico actual.
