package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseDefinition
import com.siaa.core.model.ExerciseType
import com.siaa.core.model.KcDomain
import com.siaa.core.model.LearnerKcState
import com.siaa.core.model.KnowledgeComponent
import com.siaa.core.model.ResponseConfidence
import kotlin.math.exp

class StateUpdater(
    private val bktParams: BktParams = BktParams()
) {
    fun update(
        component: KnowledgeComponent,
        prior: LearnerKcState,
        exercise: ExerciseDefinition,
        correct: Boolean,
        confidence: ResponseConfidence?,
        latencyMs: Long?,
        nowEpochMs: Long,
        evidenceWeight: Double = 1.0
    ): LearnerKcState {
        val evidence = evidenceWeight.coerceIn(0.10, 1.0)
        val confidenceWeight = when (confidence) {
            ResponseConfidence.CORRECT, null -> 1.0
            ResponseConfidence.UNSURE -> 0.65
            ResponseConfidence.WRONG -> 0.35
        } * evidence
        val latencyPenalty = latencyPenalty(latencyMs, exercise.estimatedSeconds)
        val observationCorrect = correct && confidence != ResponseConfidence.WRONG
        val rawMastery = BktUpdater.posterior(prior.mastery, observationCorrect, bktParams)
        val mastery = (prior.mastery + (rawMastery - prior.mastery) * confidenceWeight).coerceIn(0.0, 1.0)
        val newHalfLife = HalfLifeModel.updateHalfLife(
            prior.halfLifeHours,
            observationCorrect,
            confidenceWeight,
            latencyPenalty
        )
        val skillGain = if (observationCorrect) 0.09 * confidenceWeight else -0.07 * evidence
        val recognition = when (exercise.type) {
            ExerciseType.AB, ExerciseType.LISTENING_AB, ExerciseType.MEANING_AB,
            ExerciseType.PRON_DISCRIMINATION, ExerciseType.CHUNK_AB -> (prior.recognition + skillGain).coerceIn(0.0, 1.0)
            else -> prior.recognition
        }
        val production = when (exercise.type) {
            ExerciseType.SELF_ASSESS, ExerciseType.SPELL_FROM_AUDIO -> (prior.production + skillGain * 0.9).coerceIn(0.0, 1.0)
            else -> prior.production
        }
        val orthography = when {
            component.domain == KcDomain.ORTHOGRAPHY || component.domain == KcDomain.LETTER ->
                (prior.orthography + skillGain).coerceIn(0.0, 1.0)
            exercise.type == ExerciseType.SPELLING_AB || exercise.type == ExerciseType.SPELL_FROM_AUDIO ->
                (prior.orthography + skillGain).coerceIn(0.0, 1.0)
            else -> prior.orthography
        }
        val autoDelta = if (observationCorrect) 0.06 * (1.0 - latencyPenalty) * evidence else -0.05 * evidence
        val automaticity = (prior.automaticity + autoDelta).coerceIn(0.0, 1.0)
        val uncertaintyFactor = if (observationCorrect) (1.0 - 0.08 * evidence) else (1.0 - 0.04 * evidence)
        val uncertainty = (prior.uncertainty * uncertaintyFactor).coerceIn(0.04, 0.95)
        return prior.copy(
            mastery = mastery,
            recognition = recognition,
            production = production,
            orthography = orthography,
            automaticity = automaticity,
            halfLifeHours = newHalfLife,
            uncertainty = uncertainty,
            lastReviewedAtEpochMs = nowEpochMs,
            consecutiveSuccess = if (observationCorrect) prior.consecutiveSuccess + 1 else 0,
            consecutiveFailure = if (observationCorrect) 0 else prior.consecutiveFailure + 1,
            totalAttempts = prior.totalAttempts + 1,
            totalCorrect = prior.totalCorrect + if (observationCorrect) 1 else 0
        )
    }

    private fun latencyPenalty(latencyMs: Long?, estimatedSeconds: Int): Double {
        if (latencyMs == null) return 0.35
        val expectedMs = (estimatedSeconds.coerceAtLeast(3) * 1000.0 * 0.28).coerceAtLeast(1500.0)
        val ratio = latencyMs / expectedMs
        return (1.0 - exp(-0.6 * (ratio - 0.6).coerceAtLeast(0.0))).coerceIn(0.0, 1.0)
    }
}
