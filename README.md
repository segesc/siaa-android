# SIAA Android

[![Validación](https://github.com/segesc/siaa-android/actions/workflows/validate.yml/badge.svg)](https://github.com/segesc/siaa-android/actions/workflows/validate.yml)

Versión fuente **0.7.0** · Kotlin · Jetpack Compose · Android

[Primeros pasos](START_HERE.md) · [Documentación](docs/README.md) · [Contribuir](CONTRIBUTING.md) · [Cambios](CHANGELOG.md)

> Estado: código fuente importado y comprobaciones de estructura/contenido verificadas. Compilación del APK y pruebas con dispositivos pendientes. Consulta [la validación de esta importación](docs/REPOSITORY_SETUP.md).

**SIAA - Sistema de Inglés Auditivo Adaptativo** es una app Android offline-first diseñada para estudiar inglés en transporte con el teléfono guardado y la pantalla apagada. La interacción principal usa comandos multimedia que el firmware de los audífonos expone a Android (`Play/Pause`, `Next`, `Previous`).

Esta entrega contiene una implementación extensa de la arquitectura central: Android multimódulo, `MediaSessionService`, ancla silenciosa de Media3 para mantener una sesión multimedia activa, TTS bilingüe, Room, runtime pedagógico, grafo de conocimiento, planificación adaptativa, BKT, half-life, ensemble de memoria, filtro de partículas, Q-matrix, lookahead tipo POMDP, Thompson Sampling restringido, wheel-spinning, vocabulario/ortografía, historial, mapa curricular, calibración de audífonos y generación controlada de variantes.

## Módulos

- `:app` - UI Compose, servicio Media3, routing de botones, preferencias y composición de dependencias.
- `:core:model` - modelos de dominio sin Android.
- `:core:algorithm` - BKT, HLR/memoria, Knowledge Space, Q-matrix/CDM, MIRT, SMC, utility planner, POMDP aproximado, Thompson Sampling, checkpoints y wheel-spinning.
- `:core:runtime` - máquina de estados pedagógica, help ladder y protocolo de sesión.
- `:core:data` - Room, content packs versionados, repositorio local e historial.
- `:core:audio` - TextToSpeech, velocidad adaptada, earcons, audio focus y protección de rutas.
- `:core:content` - validación del currículo y generación controlada/auditable de variantes.

## Implementado

1. Currículo local base de **87 KCs y 197 relaciones**.
2. **134 ejercicios curados + ~156 variantes generadas** desde 30 lexemas curados al instalar el content pack.
3. Modos `ADAPTIVE`, `VOCABULARY`, `SPELLING`, `GRAMMAR`, `LISTENING` y `PRONUNCIATION`.
4. `MediaSessionService` foreground y player Media3 silencioso en loop como ancla de sesión para controles externos.
5. Protocolo contextual de audífonos: A/B, sí/dudé/no, repetir/ayuda y stop.
6. Help ladder: repetir -> escuchar lento -> segmentar por palabras -> deletreo cuando existe palabra objetivo.
7. TTS es-PE/en-US con control de velocidad y earcons.
8. Room para sesiones, interacciones, latencia, pistas, estado del alumno, perfiles de audífonos y misconceptions.
9. Estado multidimensional por KC: mastery, reconocimiento, producción, ortografía, automaticidad, half-life e incertidumbre.
10. Actualización conjunta con BKT + half-life + evidencia Q-matrix + posterior SMC/particle filter.
11. Planificador con olvido, dificultad, information value, readiness, unlock value, riesgo, wheel-spinning y exploración Thompson restringida.
12. Lookahead tipo POMDP de un paso sobre las acciones ya válidas pedagógicamente.
13. Content pack versionado y validado antes de insertarse (IDs, referencias, dificultad, ciclos de prerrequisitos).
14. Generador determinista de ejercicios léxicos: significado, spelling A/B, recuperación ortográfica y chunks.
15. Referencias abiertas CEFR-J incluidas bajo `assets/reference/cefrj/` con su aviso de uso.
16. UI: Inicio, Progreso + historial, Mapa curricular, Audífonos/calibración y Ajustes persistentes.
17. Scripts de QA para estructura, contenido, perfil CEFR-J y tres smoke tests Kotlin puros.

## Build

Requisitos recomendados:

- Android Studio estable reciente.
- JDK 17.
- Android SDK Platform 37.
- Gradle 9.6.0 / AGP 9.4.0.

El ZIP incluye `gradle-wrapper.properties` y scripts de bootstrap. Si falta `gradle-wrapper.jar`:

```bash
./scripts/bootstrap_gradle.sh tasks
```

Windows:

```powershell
./scripts/bootstrap_gradle.ps1 tasks
```

Después:

```bash
./gradlew :app:assembleDebug
```

## QA local sin Android SDK

```bash
python scripts/validate_project.py
./scripts/smoke_pure_kotlin.sh
./scripts/smoke_runtime.sh
./scripts/smoke_content.sh
python scripts/profile_audit.py
```

El entorno de generación no dispone de Android SDK, por lo que no se afirma que `assembleDebug` haya sido ejecutado aquí. Los módulos Kotlin puros, el runtime y el módulo de contenido sí fueron compilados/ejecutados mediante smoke tests.

Empieza por `START_HERE.md` y después revisa `docs/ARCHITECTURE.md`, `docs/MEDIA_BUTTONS.md`, `docs/LESSON_RUNTIME.md` y `docs/BUILD_AND_RUN.md`.

## Estructura del repositorio

```text
app/                Aplicación Android, UI, servicio multimedia y assets
core/               Módulos de modelo, algoritmo, runtime, datos, audio y contenido
docs/               Guías técnicas y estado de implementación
gradle/             Catálogo de versiones y configuración del wrapper
reference/          Documento maestro y checksum del PDF
research_sources/   Procedencia de fuentes
scripts/            Validación, auditoría y bootstrap
tools/              Programas de smoke test Kotlin
.github/            Workflows y plantillas de colaboración
```

## Automatización

- **Validación**: comprueba estructura, contenido e integridad del PDF en cada push y pull request; publica la auditoría CEFR-J como artefacto informativo.
- **Android CI**: ejecución manual desde Actions → Android CI → Run workflow para instalar el SDK, ejecutar tests y compilar el APK debug. Su éxito no está verificado en esta importación.

## Licencia y referencias

El ZIP no contiene una licencia general del proyecto. No se asigna una licencia nueva durante esta importación. Se conservan los avisos de las referencias incluidas.
