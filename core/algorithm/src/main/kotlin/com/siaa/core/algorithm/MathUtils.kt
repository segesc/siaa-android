package com.siaa.core.algorithm

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.random.Random

object MathUtils {
    fun binaryEntropy(pRaw: Double): Double {
        val p = pRaw.coerceIn(1e-9, 1.0 - 1e-9)
        return -(p * ln(p) + (1.0 - p) * ln(1.0 - p)) / ln(2.0)
    }

    fun sigmoid(x: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-x))

    fun betaSample(alpha: Double, beta: Double, random: Random = Random.Default): Double {
        val x = gammaSample(alpha.coerceAtLeast(0.05), random)
        val y = gammaSample(beta.coerceAtLeast(0.05), random)
        return if (x + y == 0.0) 0.5 else x / (x + y)
    }

    private fun gammaSample(shape: Double, random: Random): Double {
        if (shape < 1.0) {
            val u = random.nextDouble().coerceAtLeast(1e-12)
            return gammaSample(shape + 1.0, random) * u.pow(1.0 / shape)
        }
        val d = shape - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            val x = gaussian(random)
            var v = 1.0 + c * x
            if (v <= 0.0) continue
            v *= v * v
            val u = random.nextDouble()
            if (u < 1.0 - 0.0331 * x * x * x * x) return d * v
            if (ln(u) < 0.5 * x * x + d * (1.0 - v + ln(v))) return d * v
        }
    }

    private fun gaussian(random: Random): Double {
        val u1 = random.nextDouble().coerceAtLeast(1e-12)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}
