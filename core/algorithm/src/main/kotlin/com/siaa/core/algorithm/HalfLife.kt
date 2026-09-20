package com.siaa.core.algorithm

import kotlin.math.ln
import kotlin.math.pow

object HalfLifeModel {
    fun recallProbability(elapsedHours: Double, halfLifeHours: Double): Double {
        if (elapsedHours <= 0.0) return 1.0
        val h = halfLifeHours.coerceAtLeast(0.05)
        return 2.0.pow(-elapsedHours / h).coerceIn(0.0, 1.0)
    }

    fun updateHalfLife(
        currentHalfLifeHours: Double,
        correct: Boolean,
        confidenceWeight: Double = 1.0,
        latencyPenalty: Double = 0.0
    ): Double {
        val h = currentHalfLifeHours.coerceAtLeast(0.25)
        val gain = if (correct) 1.55 else 0.62
        val weightedGain = 1.0 + (gain - 1.0) * confidenceWeight.coerceIn(0.25, 1.25)
        val latencyFactor = (1.0 - 0.22 * latencyPenalty.coerceIn(0.0, 1.0)).coerceAtLeast(0.65)
        return (h * weightedGain * latencyFactor).coerceIn(0.25, 24.0 * 365.0)
    }

    fun hoursUntilTargetRecall(halfLifeHours: Double, targetRecall: Double): Double {
        val target = targetRecall.coerceIn(0.01, 0.99)
        return -halfLifeHours * ln(target) / ln(2.0)
    }
}
