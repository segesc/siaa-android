# Arquitectura

```text
Bluetooth earbuds
      ↓ comandos multimedia
MediaSessionService (:app)
      ↓ RuntimeCommand
LessonRuntime (:core:runtime)
      ├── SpeechPort / EarconPort (:core:audio)
      ├── LearningRepository (:core:data)
      └── ExercisePlanner (:core:algorithm)
              ↓
        Knowledge graph + BKT/HLR + Q-matrix + utility/POMDP
              ↓
        ExerciseDefinition
              ↓
           audio TTS
```

## Principios

- Offline-first.
- El dominio lingüístico no conoce Android.
- El runtime no conoce Room ni TextToSpeech concretos: usa puertos.
- El algoritmo no conoce Bluetooth ni UI.
- Los botones del audífono se interpretan según el estado pedagógico.
- Cada interacción genera evidencia persistente.
- El currículo limita qué acciones son pedagógicamente válidas antes de optimizar utilidad.

## Fronteras

`core:model` y `core:algorithm` son Kotlin puro y pueden probarse fuera de Android.
`core:runtime` también es Kotlin puro salvo su dependencia en coroutines.
`core:data` y `core:audio` son adaptadores Android.
`app` es el composition root y la interfaz del sistema operativo.

## Cambios v0.7

- `:core:content` separa contenido de infraestructura y permite QA antes de tocar Room.
- El `LessonRuntime` asigna evidencia a múltiples KCs mediante Q-matrix en lugar de actualizar todos por igual.
- `OnlineAdaptiveModels` comparte entre planner/runtime un ensemble de memoria, un SMC/particle posterior y arms de Thompson Sampling. Room sigue siendo la fuente persistente principal.
- El servicio Media3 mantiene un WAV local silencioso en loop y volumen cero mientras la sesión/calibración está activa; el audio pedagógico se reproduce por TTS. Esto conserva una sesión multimedia activa sin mezclar contenido pedagógico en el Player.
- `Previous` activa una escalera de ayuda contextual: repetir -> lento -> segmentación -> spelling.
- Los content packs son versionados; las definiciones se reemplazan al subir de versión, mientras los estados del alumno permanecen en su tabla independiente.
