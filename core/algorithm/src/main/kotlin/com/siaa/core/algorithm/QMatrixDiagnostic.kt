package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseDefinition
import com.siaa.core.model.LearnerKcState

/**
 * Diagnóstico cognitivo no compensatorio ligero inspirado en DINA/G-DINA.
 * Cada ejercicio declara explícitamente los KCs que requiere (Q-matrix implícita).
 * El modelo estima la probabilidad de respuesta correcta como producto de dominio
 * de los atributos requeridos, con slip/guess. Luego reparte evidencia posterior
 * a cada atributo sin asumir que todos fallaron por igual.
 */
class QMatrixDiagnostic(
    private val slip: Double = 0.08,
    private val guess: Double = 0.18
) {
    fun predictedCorrect(exercise: ExerciseDefinition, states: Map<String, LearnerKcState>): Double {
        if (exercise.kcIds.isEmpty()) return 0.5
        val conjunctiveMastery = exercise.kcIds
            .map { states[it]?.mastery ?: 0.15 }
            .fold(1.0) { acc, v -> acc * v.coerceIn(0.01, 0.999) }
        return (conjunctiveMastery * (1.0 - slip) + (1.0 - conjunctiveMastery) * guess)
            .coerceIn(0.01, 0.99)
    }

    fun diagnosticWeights(exercise: ExerciseDefinition, states: Map<String, LearnerKcState>): Map<String, Double> {
        if (exercise.kcIds.isEmpty()) return emptyMap()
        val raw = exercise.kcIds.associateWith { kc ->
            val m = states[kc]?.mastery ?: 0.15
            // KCs más inciertos y menos dominados reciben mayor crédito diagnóstico.
            val u = states[kc]?.uncertainty ?: 0.45
            (1.0 - m).coerceAtLeast(0.05) * u.coerceAtLeast(0.10)
        }
        val z = raw.values.sum().takeIf { it > 0.0 } ?: 1.0
        return raw.mapValues { it.value / z }
    }
}
