package com.siaa.core.data.migration

/**
 * Regla del proyecto: nunca usar destructive migration para estados del alumno.
 * Las definiciones curriculares se versionan por ContentSeeder; learner state e historial
 * deben preservarse mediante migraciones Room explícitas al cambiar el schema.
 */
object SchemaPolicy {
    const val PRESERVE_LEARNER_HISTORY: Boolean = true
}
