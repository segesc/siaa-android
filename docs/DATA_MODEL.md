# Persistencia Room

Tablas:

- `knowledge_components`
- `knowledge_edges`
- `learner_kc_state`
- `exercises`
- `sessions`
- `interactions`
- `device_profiles`
- `misconceptions`
- `app_meta`

Cada interacción persiste respuesta, corrección, confianza, latencia, profundidad de ayuda, score del planificador y mastery antes/después.

El contenido inicial se carga desde JSON sólo si la base está vacía. Para migraciones futuras, aumentar `content_version` y crear un importador incremental; no usar destructive migration en producción sin estrategia de backup.
