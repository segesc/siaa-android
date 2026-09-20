package com.siaa.core.algorithm

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

data class MemoryPrediction(
    val probability: Double,
    val componentPredictions: Map<String, Double>,
    val weights: Map<String, Double>
)

/**
 * Ensemble pequeño para no asumir una única curva de olvido.
 * HLR-like half-life, exponential y power-law compiten mediante log-loss prequential.
 */
class MemoryEnsemble(
    initialWeights: Map<String, Double> = mapOf("halfLife" to 0.50, "exponential" to 0.30, "power" to 0.20)
) {
    private val scores = initialWeights.mapValues { ln(it.value.coerceAtLeast(1e-6)) }.toMutableMap()

    fun predict(elapsedHours: Double, halfLifeHours: Double): MemoryPrediction {
        val h = halfLifeHours.coerceAtLeast(0.1)
        val p = mapOf(
            "halfLife" to HalfLifeModel.recallProbability(elapsedHours, h),
            "exponential" to exp(-ln(2.0) * elapsedHours / h).coerceIn(0.0, 1.0),
            "power" to (1.0 + elapsedHours / h).pow(-0.85).coerceIn(0.0, 1.0)
        )
        val weights = normalizedWeights()
        val mix = p.entries.sumOf { (k, v) -> v * (weights[k] ?: 0.0) }
        return MemoryPrediction(mix.coerceIn(0.001, 0.999), p, weights)
    }

    fun observe(prediction: MemoryPrediction, remembered: Boolean, learningRate: Double = 0.20) {
        for ((name, pRaw) in prediction.componentPredictions) {
            val p = pRaw.coerceIn(1e-4, 1.0 - 1e-4)
            val logLikelihood = if (remembered) ln(p) else ln(1.0 - p)
            scores[name] = (scores[name] ?: 0.0) + learningRate * logLikelihood
        }
        // Recentrar evita underflow a largo plazo.
        val max = scores.values.maxOrNull() ?: 0.0
        scores.replaceAll { _, v -> v - max }
    }

    private fun normalizedWeights(): Map<String, Double> {
        val expScores = scores.mapValues { exp(it.value) }
        val z = expScores.values.sum().takeIf { it > 0.0 } ?: 1.0
        return expScores.mapValues { it.value / z }
    }
}
