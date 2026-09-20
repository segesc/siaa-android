package com.siaa.core.algorithm

import com.siaa.core.model.InteractionRecord

data class WheelSpinningSignal(
    val detected: Boolean,
    val failureRate: Double,
    val meanLatencyMs: Double,
    val recommendation: String
)

object WheelSpinningDetector {
    fun detect(kcExerciseIds: Set<String>, interactions: List<InteractionRecord>): WheelSpinningSignal {
        val xs = interactions.filter { it.exerciseId in kcExerciseIds }.take(10)
        if (xs.size < 5) return WheelSpinningSignal(false, 0.0, 0.0, "insufficient_evidence")
        val failureRate = xs.count { !it.correct }.toDouble() / xs.size
        val lat = xs.mapNotNull { it.latencyMs }.average().takeIf { !it.isNaN() } ?: 0.0
        val detected = failureRate >= 0.60 && xs.take(4).count { !it.correct } >= 3
        val rec = if (detected) "backtrack_or_change_representation" else "continue"
        return WheelSpinningSignal(detected, failureRate, lat, rec)
    }
}
