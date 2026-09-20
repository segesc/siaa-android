package com.siaa.core.algorithm

import com.siaa.core.model.LearnerKcState

data class CheckpointResult(
    val passed: Boolean,
    val score: Double,
    val reasons: List<String>
)

object MasteryCheckpointEvaluator {
    fun evaluate(state: LearnerKcState, nowEpochMs: Long): CheckpointResult {
        val elapsed = state.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: Double.POSITIVE_INFINITY
        val retention = if (elapsed.isFinite()) HalfLifeModel.recallProbability(elapsed, state.halfLifeHours) else 0.0
        val score = 0.42 * state.mastery + 0.20 * state.recognition + 0.15 * state.production + 0.13 * state.automaticity + 0.10 * retention
        val reasons = buildList {
            if (state.mastery < 0.85) add("mastery<0.85")
            if (retention < 0.75) add("retention<0.75")
            if (state.automaticity < 0.55) add("automaticity<0.55")
            if (state.totalAttempts < 3) add("insufficient_evidence")
        }
        return CheckpointResult(reasons.isEmpty() && score >= 0.78, score, reasons)
    }
}
