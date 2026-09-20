package com.siaa.core.algorithm

import kotlin.math.max
import kotlin.math.min

data class BktParams(
    val learn: Double = 0.12,
    val guess: Double = 0.20,
    val slip: Double = 0.08,
    val forget: Double = 0.01
)

object BktUpdater {
    fun posterior(prior: Double, correct: Boolean, params: BktParams = BktParams()): Double {
        val p = prior.coerceIn(1e-6, 1.0 - 1e-6)
        val likelihoodKnown = if (correct) 1.0 - params.slip else params.slip
        val likelihoodUnknown = if (correct) params.guess else 1.0 - params.guess
        val numerator = likelihoodKnown * p
        val denominator = numerator + likelihoodUnknown * (1.0 - p)
        val observed = if (denominator <= 0.0) p else numerator / denominator
        val learned = observed + (1.0 - observed) * params.learn
        val forgotten = learned * (1.0 - params.forget)
        return forgotten.coerceIn(0.0, 1.0)
    }

    fun successProbability(mastery: Double, params: BktParams = BktParams()): Double =
        (mastery * (1.0 - params.slip) + (1.0 - mastery) * params.guess).coerceIn(0.0, 1.0)
}
