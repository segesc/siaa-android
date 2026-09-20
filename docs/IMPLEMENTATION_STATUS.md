# Estado de implementación v0.7

## Implementado en código
- Proyecto Android multimódulo y configuración Gradle.
- UI Compose: Inicio, Progreso, Mapa curricular, Calibración de audífonos y Ajustes.
- MediaSessionService foreground y routing de comandos multimedia.
- TTS bilingüe, earcons, audio focus y guardia de ruta de audio.
- LessonRuntime con estados, preguntas A/B, autoevaluación, feedback, ayuda progresiva y cierre.
- Persistencia Room de currículo, learner state, sesiones, interacciones, misconceptions y device profiles.
- Content pack versionado + validador + generador de variantes.
- Modos Adaptativo, Vocabulario, Ortografía, Gramática, Listening y Pronunciación.
- Algoritmos BKT, HLR, memory ensemble, Q-matrix, MIRT, particle filter, Thompson bandit, wheel-spinning, readiness y POMDP lookahead aproximado.
- Dashboard e historial local.
- Pruebas/smoke tests Kotlin puros y scripts de QA.

## Lo que requiere hardware/Android Studio para cerrar release
- Ejecutar `:app:assembleDebug` con Android SDK 37 instalado.
- Probar routing de MediaSession con el modelo real de audífonos.
- Pruebas instrumentadas en Android físico con pantalla bloqueada.
- Verificar comportamiento OEM de foreground service / ahorro de batería.
- Sustituir o complementar TTS por clips curados para contrastes fonológicos críticos.

## Lo que requiere datos longitudinales reales
Los pesos finales del planificador, priors, umbrales de checkpoints y selección de estrategia no deben congelarse antes de obtener interacciones reales. El código deja estos componentes separables para recalibrarlos sin reescribir la app.

## Alcance del contenido incluido
El motor está preparado para escalar. El content pack incluido es una batería de ingeniería suficiente para ejecutar y validar todos los tipos principales de interacción, no un catálogo final de miles de actividades.
