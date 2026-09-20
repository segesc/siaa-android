package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseType
import kotlin.random.Random

/**
 * Estado online ligero de modelos que aprenden durante una sesión.
 * El estado persistente principal sigue en Room (mastery, half-life, etc.).
 * Estos adaptadores pueden reiniciarse tras process death sin perder el modelo pedagógico base.
 */
class OnlineAdaptiveModels(
    private val random: Random = Random.Default
) {
    private val memoryModels = mutableMapOf<String, MemoryEnsemble>()
    private val particleFilters = mutableMapOf<String, ParticleMasteryFilter>()
    private val banditArms = mutableMapOf<String, BanditArm>()

    fun recallProbability(kcId: String, elapsedHours: Double, halfLifeHours: Double): Double =
        memoryModels.getOrPut(kcId) { MemoryEnsemble() }
            .predict(elapsedHours, halfLifeHours)
            .probability

    fun observeMemory(kcId: String, elapsedHours: Double, halfLifeHours: Double, remembered: Boolean) {
        val model = memoryModels.getOrPut(kcId) { MemoryEnsemble() }
        val prediction = model.predict(elapsedHours, halfLifeHours)
        model.observe(prediction, remembered)
    }


    fun observeLatentState(
        kcId: String,
        priorMastery: Double,
        priorHalfLifeHours: Double,
        correct: Boolean,
        elapsedHours: Double
    ): ParticlePosterior {
        val filter = particleFilters.getOrPut(kcId) {
            ParticleMasteryFilter(
                initialMastery = priorMastery,
                initialHalfLifeHours = priorHalfLifeHours,
                random = random
            )
        }
        return filter.observe(correct, elapsedHours)
    }

    /** Thompson sampling restringido: sólo compara variantes ya válidas pedagógicamente. */
    fun strategySample(kcId: String, type: ExerciseType): Double {
        val key = armKey(kcId, type)
        val arm = banditArms.getOrPut(key) { BanditArm(key) }
        return MathUtils.betaSample(arm.alpha, arm.beta, random)
    }

    fun observeStrategy(kcId: String, type: ExerciseType, reward: Double) {
        val key = armKey(kcId, type)
        val prior = banditArms.getOrPut(key) { BanditArm(key) }
        banditArms[key] = ThompsonBandit(random).update(prior, reward)
    }

    fun strategyMean(kcId: String, type: ExerciseType): Double {
        val arm = banditArms[armKey(kcId, type)] ?: return 0.5
        return arm.alpha / (arm.alpha + arm.beta)
    }

    private fun armKey(kcId: String, type: ExerciseType) = "$kcId::${type.name}"
}
