# Changelog

## 0.7.0
- Nuevo módulo `:core:content` con validador y generador controlado de variantes.
- Content pack versionado y reemplazable sin borrar el estado del alumno.
- 30 lexemas curados generan ~156 actividades adicionales sobre 134 estáticas.
- Integración efectiva de Q-matrix en la atribución de evidencia por KC.
- Memory ensemble usado por el planificador y actualizado online.
- Particle filter/SMC activado como segunda estimación de mastery y half-life.
- Thompson Sampling restringido activado para explorar tipos de actividad ya válidos.
- Wheel-spinning conectado a boost de remediación y penalización de práctica repetitiva.
- Help ladder de cuatro niveles: repetir, lento, segmentar, spelling.
- `SpeechPort` soporta velocidad relativa por intervención.
- Preferencias persistentes: máximo de actividades, anuncios de controles y explicaciones.
- Modo de calibración de audífonos independiente de una lección.
- Ancla silenciosa Media3 en loop para mantener la sesión multimedia activa durante TTS.
- Historial de sesiones y mapa curricular con readiness/unlock en Compose.
- Datos CEFR-J abiertos incorporados como referencia y script de auditoría.
- `smoke_content.sh` y validación de ciclos duros en prerrequisitos.

## 0.6.0
- Reestructuración multimódulo completa.
- Room y seeding de 87 KCs / 197 edges / 134 ejercicios.
- Runtime adaptativo genérico.
- MediaSessionService y media button router.
- Modos Adaptive/Vocabulary/Spelling/Grammar/Listening/Pronunciation.
- BKT, HLR, memory ensemble, Q-matrix, MIRT, particle filter, POMDP lookahead.
- UI Compose para inicio/progreso/audífonos/ajustes.
- Vocabulario con ortografía y deletreo.
- Scripts de validación y smoke test.

## v0.7-final
- Corregida la firma de `HomeScreen` para compilar con su llamada actual.
- Referencia actualizada al Documento Maestro v1.5.
- Añadidos `TECH_STACK.md`, `IMPLEMENTATION_STATUS.md` y verificación de release.
- Añadido workflow CI para SDK Android, tests y `assembleDebug`.
- Añadida política de sesión y política explícita de migraciones de datos.
