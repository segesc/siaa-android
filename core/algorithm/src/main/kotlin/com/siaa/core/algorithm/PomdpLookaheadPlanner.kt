package com.siaa.core.algorithm

import com.siaa.core.model.*

/**
 * Aproximación práctica al POMDP: usa el ranking inmediato para generar un conjunto pequeño
 * de acciones y añade el valor esperado de un paso futuro bajo dos observaciones posibles
 * (correcto / incorrecto). No pretende resolver exactamente el POMDP completo; permite
 * sustituir este módulo por MCTS/POMCP sin tocar runtime, datos ni UI.
 */
class PomdpLookaheadPlanner(
    private val base: ExercisePlanner = AdaptiveUtilityPlanner(),
    private val gamma: Double = 0.55,
    private val branchWidth: Int = 6
) : ExercisePlanner {
    override fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int
    ): List<PlannerCandidate> {
        val immediate = base.rank(mode, snapshot, recentInteractions, nowEpochMs, branchWidth)
        return immediate.map { candidate ->
            val exercise = candidate.exercise
            val kcId = exercise.kcIds.firstOrNull() ?: return@map candidate
            val prior = snapshot.stateByKcId[kcId] ?: LearnerKcState(kcId)
            val params = BktParams()
            val pCorrect = candidate.successProbability
            val masteredIfCorrect = BktUpdater.posterior(prior.mastery, true, params)
            val masteredIfWrong = BktUpdater.posterior(prior.mastery, false, params)
            val expectedMastery = pCorrect * masteredIfCorrect + (1.0 - pCorrect) * masteredIfWrong
            val futureGain = (expectedMastery - prior.mastery).coerceAtLeast(-0.15)
            candidate.copy(
                utility = candidate.utility + gamma * futureGain,
                rationale = candidate.rationale + ", lookaheadGain=${"%.3f".format(java.util.Locale.US, futureGain)}"
            )
        }.sortedByDescending { it.utility }.take(limit)
    }
}
