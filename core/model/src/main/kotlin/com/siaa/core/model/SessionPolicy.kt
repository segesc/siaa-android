package com.siaa.core.model

data class SessionPolicy(
    val targetRecall: Double = 0.82,
    val masteryThreshold: Double = 0.85,
    val readinessThreshold: Double = 0.72,
    val maxHelpDepth: Int = 4,
    val newContentFraction: Double = 0.25,
    val reviewFraction: Double = 0.50,
    val transferFraction: Double = 0.25
) {
    init {
        require(targetRecall in 0.0..1.0)
        require(masteryThreshold in 0.0..1.0)
        require(readinessThreshold in 0.0..1.0)
        require(maxHelpDepth >= 0)
        require(newContentFraction >= 0 && reviewFraction >= 0 && transferFraction >= 0)
    }
}
