package com.siaa.core.algorithm

import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

data class MasteryParticle(val mastery: Double, val halfLifeHours: Double, val weight: Double)

data class ParticlePosterior(val masteryMean: Double, val halfLifeMean: Double, val masterySd: Double)

/**
 * Filtro secuencial Monte Carlo deliberadamente compacto. Útil para representar
 * incertidumbre cuando la media BKT es insuficiente. No se ejecuta por defecto
 * en cada turno todavía; queda listo para activar por experimento.
 */
class ParticleMasteryFilter(
    particleCount: Int = 128,
    initialMastery: Double = 0.15,
    initialHalfLifeHours: Double = 8.0,
    private val random: Random = Random.Default
) {
    private var particles: List<MasteryParticle> = List(particleCount) {
        val masteryJitter = (random.nextDouble() - 0.5) * 0.18
        val halfLifeMultiplier = exp((random.nextDouble() - 0.5) * 0.70)
        MasteryParticle(
            mastery = (initialMastery + masteryJitter).coerceIn(0.001, 0.999),
            halfLifeHours = (initialHalfLifeHours * halfLifeMultiplier).coerceIn(0.25, 24.0 * 365.0),
            weight = 1.0 / particleCount
        )
    }

    fun observe(correct: Boolean, elapsedHours: Double): ParticlePosterior {
        val weighted = particles.map { p ->
            val recall = HalfLifeModel.recallProbability(elapsedHours, p.halfLifeHours)
            val success = (0.18 + 0.78 * p.mastery * recall).coerceIn(0.02, 0.98)
            val likelihood = if (correct) success else 1.0 - success
            p.copy(weight = p.weight * likelihood)
        }
        val z = weighted.sumOf { it.weight }.takeIf { it > 1e-15 } ?: 1.0
        particles = systematicResample(weighted.map { it.copy(weight = it.weight / z) })
        return posterior()
    }

    fun posterior(): ParticlePosterior {
        val mean = particles.sumOf { it.mastery * it.weight }
        val h = particles.sumOf { it.halfLifeHours * it.weight }
        val varM = particles.sumOf { it.weight * (it.mastery - mean) * (it.mastery - mean) }
        return ParticlePosterior(mean, h, sqrt(varM.coerceAtLeast(0.0)))
    }

    private fun systematicResample(source: List<MasteryParticle>): List<MasteryParticle> {
        val n = source.size
        val positions = DoubleArray(n) { (random.nextDouble() + it) / n }
        val cumulative = DoubleArray(n)
        var c = 0.0
        for (i in source.indices) { c += source[i].weight; cumulative[i] = c }
        var j = 0
        return positions.map { pos ->
            while (j < n - 1 && cumulative[j] < pos) j++
            val base = source[j]
            val jitter = (random.nextDouble() - 0.5) * 0.025
            val hJitter = exp((random.nextDouble() - 0.5) * 0.08)
            base.copy(
                mastery = (base.mastery + jitter).coerceIn(0.001, 0.999),
                halfLifeHours = (base.halfLifeHours * hJitter).coerceIn(0.25, 24.0 * 365.0),
                weight = 1.0 / n
            )
        }
    }
}
