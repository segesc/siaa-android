# Máquina de estados

```text
IDLE
 ↓ start
PREPARING
 ↓
SPEAKING
 ├─ TEACH ───────────────→ next
 ├─ binary ─→ WAITING_BINARY
 └─ recall ─→ WAITING_SELF_ASSESSMENT

WAITING_BINARY
  PRIMARY   = A
  SECONDARY = B
  BACK      = repetir

WAITING_SELF_ASSESSMENT
  PRIMARY   = correcto
  SECONDARY = dudé
  BACK      = fallé

respuesta → FEEDBACK → update state → planner → siguiente
```

`LessonRuntime` almacena el prompt activo para repetirlo tras pausa o `Previous`.
