# Motor adaptativo

La selección de la siguiente actividad no es una lista lineal.

## 1. Estado

Por KC se conservan, al menos:

- mastery
- recognition
- production
- orthography
- automaticity
- half-life
- uncertainty
- timestamps e historial

## 2. Restricciones de grafo

`KnowledgeGraphEngine` calcula `readiness`, `isUnlocked`, `outerFringe` y valor de desbloqueo. Los prerrequisitos duros bloquean acciones no válidas.

## 3. Memoria

`HalfLifeModel` estima recall en función de tiempo transcurrido y h½. `MemoryEnsemble` permite comparar más de una curva de olvido y actualizar pesos por evidencia prequential.

## 4. Diagnóstico

BKT actualiza dominio después de respuestas. `QMatrixDiagnostic` reparte evidencia cuando un ejercicio exige múltiples KCs. `MirtModel` queda disponible para dificultad multidimensional.

## 5. Función de utilidad

`AdaptiveUtilityPlanner` combina:

- urgencia de retención,
- mastery gap,
- automaticity gap,
- entropía/incertidumbre,
- dificultad objetivo,
- valor de desbloqueo,
- importancia curricular,
- readiness,
- duración,
- penalización de riesgo.

## 6. Lookahead

`PomdpLookaheadPlanner` añade valor esperado del posterior tras respuesta correcta/incorrecta. Es una aproximación operacional de un paso; la interfaz permite sustituirla por POMCP/MCTS.

## 7. Experimentación

`ThompsonBandit` puede escoger variantes dentro del conjunto pedagógicamente válido. Nunca decide saltarse prerrequisitos.

## Política online v0.7

La política efectiva ya conecta varias piezas que antes eran sólo componentes disponibles:

1. Memory ensemble predice recall por KC.
2. Q-matrix estima `P(correct)` y reparte evidencia sobre los KCs requeridos.
3. BKT actualiza mastery base.
4. SMC/particle filter produce una segunda posterior de mastery/half-life; ambas estimaciones se combinan de forma conservadora.
5. Wheel-spinning detecta fracaso persistente y favorece TEACH/contrast/remediation en vez de repetir la misma prueba.
6. Thompson Sampling aporta sólo un bonus pequeño entre actividades que ya pasaron restricciones curriculares.
7. El POMDP lookahead añade valor esperado de un paso futuro al ranking inmediato.

El orden es deliberado: **primero restricciones pedagógicas y readiness, después optimización estadística**.
