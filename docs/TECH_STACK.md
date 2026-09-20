# Stack tecnológico SIAA Android

## Plataforma
- Android nativo, Kotlin.
- minSdk 26, targetSdk 36, compileSdk 37.
- JDK/JVM target 17.
- Gradle 9.6 / Android Gradle Plugin 9.4.

## UI y ciclo de vida
- Jetpack Compose + Material 3.
- Lifecycle ViewModel + StateFlow.
- UI deliberadamente secundaria: la sesión principal funciona con pantalla apagada.

## Audio y audífonos
- AndroidX Media3 (`ExoPlayer`, `MediaSession`, `MediaSessionService`).
- `ACTION_MEDIA_BUTTON` / `KeyEvent` traducidos por `EarbudCommandRouter`.
- TextToSpeech local para prompts bilingües.
- Earcons para feedback rápido.
- Audio focus y `ACTION_AUDIO_BECOMING_NOISY` para pausar ante pérdida de salida.

## Datos
- Room + KSP.
- Content pack JSON versionado y validado antes de sembrar la base.
- Historial de sesiones/interacciones, estados por KC, misconceptions y perfiles de dispositivo.

## Núcleo científico
- BKT extendido como señal de dominio.
- Half-Life Regression / ensemble de memoria para retención.
- Knowledge Graph / readiness de prerrequisitos.
- Q-matrix y diagnóstico multi-KC.
- MIRT y dificultad.
- Particle filter para estado latente.
- Utility planner con riesgo, coste temporal, información y unlock value.
- Lookahead aproximado tipo POMDP/MPC.
- Thompson Sampling restringido a variantes pedagógicamente válidas.
- Wheel-spinning y remediación.

## Operación
- Offline-first.
- No telemetría remota en esta entrega.
- Fuente de verdad pedagógica: `reference/SIAA_documento_maestro_v1_5_android_v0_7.pdf`.
