# Importación a GitHub

Origen: `SIAA_Android_Full_v0_7.zip`, versión declarada 0.7.0.

Se conserva la estructura Android multimódulo, el código, los assets y el documento maestro. Se agregan navegación documental, guía de contribución, plantillas de issues/PR, reglas de formato y exclusiones para archivos locales y claves de firma.

## Validación local realizada

- `python -X utf8 scripts/validate_project.py`: correcto; 51 archivos Kotlin, 87 KCs, 197 relaciones, 134 ejercicios, 30 lexemas y unas 156 variantes estimadas.
- `python -X utf8 scripts/verify_release.py`: correcto; manifiesto y SHA-256 del PDF maestro verificados.
- `python -X utf8 scripts/profile_audit.py`: ejecutado; 28 de 30 lexemas encontrados, 20 de los 28 coinciden en nivel. Es una auditoría informativa, no una validación pedagógica aprobada.
- Revisión de patrones habituales de secretos y archivos de firma: sin coincidencias detectadas; no constituye una auditoría exhaustiva.

## Pendiente

No se ejecutaron aquí la compilación Android ni los smoke tests Kotlin por falta de Java/Kotlin/SDK en el entorno disponible. Los resultados históricos incluidos en el ZIP no se presentan como nuevas pruebas realizadas.

El workflow Android original se conserva como ejecución manual, con límite de tiempo y publicación del APK debug cuando finalice correctamente. La compatibilidad y disponibilidad de las versiones de dependencias originales aún deben validarse. El workflow de estructura/contenido se ejecuta automáticamente.

## Integridad y procedencia

`FILES_SHA256.txt` se regenera para esta importación y excluye su propio contenido para evitar una referencia circular. Los hashes originales del ZIP se conservan en `reference/ORIGINAL_FILES_SHA256.txt` únicamente como registro de procedencia; no representan los archivos modificados del repositorio.

No se añadió una licencia general porque no venía especificada en el ZIP.
