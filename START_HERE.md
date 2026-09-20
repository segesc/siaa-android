# START HERE - primera ejecución real

## Objetivo físico de v0.7

> Completar una sesión real de 20-30 minutos con el teléfono bloqueado, usando sólo los audífonos, sin perder eventos y con todas las interacciones persistidas.

## 1. Abrir y compilar

1. Abre la raíz del proyecto en Android Studio.
2. Usa JDK 17.
3. Instala Android SDK Platform 37 si se solicita.
4. Sincroniza Gradle.
5. Ejecuta `:app:assembleDebug` o Run sobre un teléfono físico.

Si el wrapper JAR no existe, usa `scripts/bootstrap_gradle.sh` o `scripts/bootstrap_gradle.ps1`.

## 2. Calibrar los audífonos antes de estudiar

1. Conecta los audífonos Bluetooth.
2. Abre **Audífonos**.
3. Pulsa **Escuchar controles**.
4. Ejecuta los gestos físicos de play/pause, siguiente y anterior.
5. Verifica qué comandos aparecen.
6. Guarda el perfil.

Android recibe comandos multimedia del firmware; no existe un evento portable universal de "doble toque" o "triple toque".

## 3. Primera sesión

1. Ve a Inicio.
2. Empieza por `GRAMMAR` o `VOCABULARY` con 10-20 actividades en Ajustes.
3. Bloquea la pantalla.
4. Completa preguntas A/B sólo con audífonos.
5. Usa Previous repetidamente para probar la escalera de ayuda.
6. Al terminar revisa **Progreso** e **Historial**.
7. Revisa **Mapa** para confirmar que cambian mastery/readiness.

## 4. Qué comprobar

- recepción de controles con pantalla apagada;
- que al desconectar audífonos se pause y no continúe por altavoz;
- que el foreground service permanezca vivo;
- que el silencio Media3 no sea audible;
- que TTS español/inglés use voces razonables;
- que Room conserve progreso después de matar y reabrir la app;
- que la latencia quede registrada;
- que el modo spelling deletree letras por sus nombres en inglés.

## 5. Restricciones deliberadas

- Sin login obligatorio.
- Sin servidor necesario para estudiar.
- Sin generación libre de contenido por IA en runtime.
- Los modelos neuronales quedan como benchmark futuro, no requisito del MVP.
- El content pack actual valida arquitectura; todavía no equivale al currículo completo Pre-A1-C2.

## 6. Después de la prueba física

El siguiente trabajo debe guiarse por datos reales: keycodes del audífono, rutas de audio, latencia, errores de TTS y fallos pedagógicos observados. No conviene expandir miles de actividades antes de verificar el bucle físico completo.
