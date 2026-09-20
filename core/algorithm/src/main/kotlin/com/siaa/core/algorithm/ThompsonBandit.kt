package com.siaa.core.algorithm

import kotlin.random.Random

data class BanditArm(val id: String, val alpha: Double = 1.0, val beta: Double = 1.0)

class ThompsonBandit(private val random: Random = Random.Default) {
    fun choose(arms: List<BanditArm>): BanditArm? = arms.maxByOrNull { MathUtils.betaSample(it.alpha, it.beta, random) }

    fun update(arm: BanditArm, reward: Double): BanditArm {
        val r = reward.coerceIn(0.0, 1.0)
        return arm.copy(alpha = arm.alpha + r, beta = arm.beta + (1.0 - r))
    }
}
