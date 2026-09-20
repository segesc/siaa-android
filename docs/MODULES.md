# Módulos

## `:app`
Android/Compose. MainActivity, foreground MediaSessionService, calibración de controles, preferencias, navegación, historial y mapa curricular.

## `:core:model`
Objetos puros: KCs, edges, estados, ejercicios, interacciones, sesiones, perfiles de audífonos y alfabeto.

## `:core:algorithm`
Inferencia y decisión: BKT, half-life, memory ensemble, filtro de partículas, Knowledge Space, Q-matrix/CDM, MIRT, utility planner, POMDP lookahead, Thompson Sampling, checkpoints, ortografía y wheel-spinning.

## `:core:runtime`
Máquina de estados de una sesión. Une planner, repositorio, TTS/earcons, botones, help ladder, feedback y actualización del learner model.

## `:core:data`
Room y adapter Android. Seed/update de content packs versionados, historial y persistencia.

## `:core:audio`
TTS Android, control de velocidad, earcons, audio focus y `ACTION_AUDIO_BECOMING_NOISY`.

## `:core:content`
Código puro para validar el grafo/ejercicios y generar variantes deterministas desde lexemas curados. No genera inglés libremente.
