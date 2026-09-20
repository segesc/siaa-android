# Contribuir a SIAA

1. Revisa [la arquitectura](docs/ARCHITECTURE.md) y [el estado de implementación](docs/IMPLEMENTATION_STATUS.md).
2. Crea una rama desde `main`: `feat/descripcion`, `fix/descripcion` o `docs/descripcion`.
3. Mantén cada cambio enfocado y respeta los límites de los módulos `core`.
4. Ejecuta `python -X utf8 scripts/validate_project.py` y `python -X utf8 scripts/verify_release.py`.
5. Si cambias vocabulario, ejecuta `python -X utf8 scripts/profile_audit.py` y revisa sus diferencias.
6. Para cambios Kotlin/Android, ejecuta los tests pertinentes según [BUILD_AND_RUN](docs/BUILD_AND_RUN.md). Indica expresamente qué no pudiste ejecutar.
7. Abre un pull request con el problema, el resultado y la evidencia de validación.

No incluyas claves de firma, secretos, rutas del SDK local ni resultados de compilación en Git. Conserva los avisos de procedencia de las referencias. Los cambios de persistencia deben detallar su migración y efecto sobre el progreso del estudiante.
