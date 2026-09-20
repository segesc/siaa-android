# Build & Run

## Toolchain fijado

- compileSdk 37
- targetSdk 36
- minSdk 26
- JDK 17
- AGP 9.4.0
- Gradle 9.6.0
- Kotlin/Compose compiler 2.2.10
- Media3 1.11.1
- Room 2.8.5

## Wrapper

El archivo binario `gradle-wrapper.jar` no pudo descargarse en el entorno de generación. Ejecuta:

```bash
./scripts/bootstrap_gradle.sh tasks
```

El script descarga el wrapper oficial v9.6.0 y verifica SHA-256 antes de ejecutarlo.

## Build

```bash
./gradlew clean :app:assembleDebug
```

APK esperado:

`app/build/outputs/apk/debug/app-debug.apk`

## Prueba mínima

- Android físico.
- TTS inglés y español instalados.
- audífonos Bluetooth.
- notificaciones permitidas en Android 13+.
