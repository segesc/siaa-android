# Pipeline de contenido

El ZIP trae un corpus inicial pequeño para probar software, no el currículo final completo.

## Archivos

- `kcs.json`
- `edges.json`
- `exercises.json`

## Regla de publicación

Una actividad no entra al catálogo final sin:

1. KCs explícitos.
2. nivel/priors documentados.
3. objetivo forma-significado-uso.
4. clave de respuesta verificable cuando sea objetiva.
5. explicación correctiva.
6. estimación de dificultad inicial.
7. tags de modalidad.
8. revisión humana de inglés y audio.

El gran catálogo futuro debe generarse desde una fuente estructurada versionada y validarse con `scripts/validate_project.py`.
