package com.siaa.core.algorithm

object OrthographicScorer {
    private val irregularClusters = listOf("ough", "augh", "igh", "eigh", "ould", "tion", "sion", "ght", "kn", "wr")

    fun score(word: String, observedErrorRate: Double = 0.0, importance: Double = 0.5): Double {
        val w = word.lowercase()
        val irregularity = irregularClusters.count { it in w }.coerceAtMost(3) / 3.0
        val length = ((w.length - 4).coerceAtLeast(0) / 8.0).coerceAtMost(1.0)
        return (0.45 * irregularity + 0.20 * length + 0.25 * observedErrorRate.coerceIn(0.0, 1.0) + 0.10 * importance.coerceIn(0.0, 1.0))
            .coerceIn(0.0, 1.0)
    }

    fun shouldSpell(word: String, observedErrorRate: Double = 0.0, importance: Double = 0.5): Boolean =
        score(word, observedErrorRate, importance) >= 0.35
}
