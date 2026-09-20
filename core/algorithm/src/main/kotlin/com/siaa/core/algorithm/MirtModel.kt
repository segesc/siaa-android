package com.siaa.core.algorithm

import kotlin.math.exp

data class MirtItem(
    val discrimination: Map<String, Double>,
    val difficulty: Double,
    val guessing: Double = 0.0
)

object MirtModel {
    /** Compensatory multidimensional 2PL-like model. */
    fun probability(theta: Map<String, Double>, item: MirtItem): Double {
        val linear = item.discrimination.entries.sumOf { (dim, a) -> a * (theta[dim] ?: 0.0) } - item.difficulty
        val logistic = 1.0 / (1.0 + exp(-linear))
        return (item.guessing + (1.0 - item.guessing) * logistic).coerceIn(0.001, 0.999)
    }
}
