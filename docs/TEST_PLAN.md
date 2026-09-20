# Plan de pruebas

## Automatizadas

- JSON referencial: IDs y KCs válidos.
- Kotlin puro: BKT, half-life, grafo y planner.
- Room: seed único, mapeos y persistencia (añadir instrumented tests cuando exista SDK).

## Hardware

Probar como mínimo dos modelos de audífonos:

1. pantalla encendida;
2. pantalla bloqueada;
3. app en background;
4. desconexión Bluetooth;
5. sacar un audífono si el modelo pausa automáticamente;
6. llamada/notificación interrumpiendo audio;
7. Next/Previous repetidos;
8. 30 minutos continuos.

## Criterio MVP

- >99 % de comandos recibidos en sesión controlada.
- cero reproducción inesperada por altavoz tras `AUDIO_BECOMING_NOISY`.
- todas las respuestas guardadas.
- sesión recuperable tras pausa.
