package com.siaa.core.model

import kotlin.math.abs

data class SessionPolicy(
    val targetRecall: Double = 0.82,
    val masteryThreshold: Double = 0.85,
    val hardPrereqThreshold: Double = 0.68,
    val readinessThreshold: Double = 0.72,
    val maxHelpDepth: Int = 4,
    val newContentFraction: Double = 0.25,
    val reviewFraction: Double = 0.50,
    val transferFraction: Double = 0.25,
    val binaryResponseTimeoutMs: Long = 12_000L,
    val selfAssessmentTimeoutMs: Long = 10_000L,
    val maxTimeoutRetries: Int = 2,
    val cefrCoverageThreshold: Double = 0.70
) {
    init {
        require(targetRecall in 0.0..1.0)
        require(masteryThreshold in 0.0..1.0)
        require(hardPrereqThreshold in 0.0..1.0)
        require(readinessThreshold in 0.0..1.0)
        require(cefrCoverageThreshold in 0.0..1.0)
        require(maxHelpDepth >= 0)
        require(newContentFraction >= 0 && reviewFraction >= 0 && transferFraction >= 0)
        require(abs(newContentFraction + reviewFraction + transferFraction - 1.0) < 1e-6) {
            "Fractions must sum to 1.0: newContent=$newContentFraction, review=$reviewFraction, transfer=$transferFraction"
        }
        require(binaryResponseTimeoutMs > 0)
        require(selfAssessmentTimeoutMs > 0)
        require(maxTimeoutRetries >= 1)
    }
}

