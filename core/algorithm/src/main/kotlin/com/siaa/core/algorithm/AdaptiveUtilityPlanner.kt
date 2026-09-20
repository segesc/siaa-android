package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.math.abs
import kotlin.math.exp

class AdaptiveUtilityPlanner(
    private val onlineModels: OnlineAdaptiveModels = OnlineAdaptiveModels(),
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic(),
    private val policy: SessionPolicy = SessionPolicy()
) : ExercisePlanner {
    override fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int
    ): List<PlannerCandidate> = rank(mode, snapshot, recentInteractions, nowEpochMs, limit, SessionCapabilities())

    fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int,
        capabilities: SessionCapabilities
    ): List<PlannerCandidate> {
        val states = snapshot.states.associateBy { it.kcId }
        val graph = KnowledgeGraphEngine(snapshot.components, snapshot.edges)
        val recentIds = recentInteractions.take(8).map { it.exerciseId }.toSet()

        val eligible = snapshot.exercises.asSequence()
            .filter { modeAllows(mode, it, snapshot.componentById) }
            .filter { capabilities.canPresent(it.type) }
            .filter { isEligible(it, states, graph, nowEpochMs) }
            .filterNot { it.id in recentIds && snapshot.exercises.size > 12 }
            .map { exercise -> score(exercise, snapshot, states, graph, recentInteractions, nowEpochMs) }
            .sortedByDescending { it.utility }
            .take(limit)
            .toList()

        if (eligible.isNotEmpty()) return eligible

        // Si todos los recientes agotaron elegibles, permitir repetir recientes pero NUNCA violar isEligible
        return snapshot.exercises.asSequence()
            .filter { modeAllows(mode, it, snapshot.componentById) }
            .filter { capabilities.canPresent(it.type) }
            .filter { isEligible(it, states, graph, nowEpochMs) }
            .map { exercise -> score(exercise, snapshot, states, graph, recentInteractions, nowEpochMs) }
            .sortedByDescending { it.utility }
            .take(limit)
            .toList()
    }

    private fun isEligible(
        exercise: ExerciseDefinition,
        states: Map<String, LearnerKcState>,
        graph: KnowledgeGraphEngine,
        nowEpochMs: Long
    ): Boolean {
        if (exercise.kcIds.isEmpty()) return true
        // TODOS los KCs del ejercicio deben tener sus prerrequisitos duros cumplidos.
        // NINGÚN tipo de ejercicio (incluyendo TEACH) puede saltarse un prerrequisito duro.
        return exercise.kcIds.all { kcId ->
            graph.isUnlocked(
                kcId = kcId,
                states = states,
                threshold = policy.hardPrereqThreshold,
                nowEpochMs = nowEpochMs,
                useCheckpoints = true
            )
        }
    }

    private fun score(
        exercise: ExerciseDefinition,
        snapshot: LearningSnapshot,
        states: Map<String, LearnerKcState>,
        graph: KnowledgeGraphEngine,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long
    ): PlannerCandidate {
        val kcCount = exercise.kcIds.size.coerceAtLeast(1)
        val diagnosticWeights = diagnostic.diagnosticWeights(exercise, states)
        val sumDiag = exercise.kcIds.sumOf { diagnosticWeights[it] ?: (1.0 / kcCount) }.coerceAtLeast(1e-6)

        var weightedRetentionUrgency = 0.0
        var weightedMasteryGap = 0.0
        var weightedAutoGap = 0.0
        var weightedInfo = 0.0
        var weightedUnlock = 0.0
        var weightedImportance = 0.0
        var weightedReadiness = 0.0
        var weightedTargetDiff = 0.0
        var weightedStrategy = 0.0
        var totalAttemptsAll = 0

        for (kcId in exercise.kcIds) {
            val w = (diagnosticWeights[kcId] ?: (1.0 / kcCount)) / sumDiag
            val component = snapshot.componentById[kcId]
            val state = states[kcId] ?: LearnerKcState(kcId, mastery = component?.priorMastery ?: 0.15)
            totalAttemptsAll += state.totalAttempts

            val elapsedHours = state.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: 9999.0
            val recall = if (state.lastReviewedAtEpochMs == null) 0.35 else onlineModels.recallProbability(kcId, elapsedHours, state.halfLifeHours)
            val retentionUrgency = (1.0 - recall).coerceIn(0.0, 1.0)
            val masteryGap = (1.0 - state.mastery).coerceIn(0.0, 1.0)
            val autoGap = (1.0 - state.automaticity).coerceIn(0.0, 1.0)
            val info = MathUtils.binaryEntropy(state.mastery) * state.uncertainty.coerceAtLeast(0.15)
            val unlock = graph.unlockValue(kcId, states)
            val importance = component?.importance ?: 0.5
            val readiness = graph.readiness(kcId, states)
            val targetDiff = (0.25 + 0.65 * state.mastery).coerceIn(0.25, 0.90)
            val strategySample = onlineModels.strategySample(kcId, exercise.type)

            weightedRetentionUrgency += w * retentionUrgency
            weightedMasteryGap += w * masteryGap
            weightedAutoGap += w * autoGap
            weightedInfo += w * info
            weightedUnlock += w * unlock
            weightedImportance += w * importance
            weightedReadiness += w * readiness
            weightedTargetDiff += w * targetDiff
            weightedStrategy += w * strategySample
        }

        val successP = diagnostic.predictedCorrect(exercise, states)
        val difficultyMatch = exp(-2.2 * abs(exercise.difficulty - weightedTargetDiff))
        val durationPenalty = (exercise.estimatedSeconds / 90.0).coerceIn(0.0, 1.0)
        val riskPenalty = when {
            successP < 0.25 && exercise.type != ExerciseType.TEACH -> 0.9
            successP < 0.40 -> 0.35
            successP > 0.96 && weightedMasteryGap < 0.12 -> 0.25
            else -> 0.0
        }
        val teachingBoost = if (exercise.type == ExerciseType.TEACH && totalAttemptsAll == 0) 0.55 else 0.0

        // Wheel spinning eval sobre todos los KCs involucrados
        val allExerciseIds = snapshot.exercises.filter { ex -> ex.kcIds.any { it in exercise.kcIds } }.map { it.id }.toSet()
        val wheel = WheelSpinningDetector.detect(allExerciseIds, recentInteractions)
        val remediationBoost = when {
            !wheel.detected -> 0.0
            exercise.type == ExerciseType.TEACH -> 0.70
            "remedial" in exercise.tags || "contrast" in exercise.tags -> 0.35
            else -> -0.45
        }
        val explorationBonus = 0.06 * (weightedStrategy - 0.5)

        val utility = (
            0.23 * weightedRetentionUrgency +
            0.19 * weightedMasteryGap +
            0.10 * weightedAutoGap +
            0.15 * weightedInfo +
            0.08 * weightedUnlock.coerceIn(0.0, 1.0) +
            0.10 * difficultyMatch +
            0.08 * weightedImportance +
            0.07 * weightedReadiness +
            teachingBoost +
            remediationBoost +
            explorationBonus -
            0.08 * durationPenalty -
            0.18 * riskPenalty
        )

        return PlannerCandidate(
            exercise = exercise,
            utility = utility,
            successProbability = successP,
            retentionUrgency = weightedRetentionUrgency,
            informationValue = weightedInfo,
            unlockValue = weightedUnlock,
            riskPenalty = riskPenalty,
            rationale = "gap=${fmt(weightedMasteryGap)}, info=${fmt(weightedInfo)}, readiness=${fmt(weightedReadiness)}, wheel=${wheel.detected}"
        )
    }

    private fun modeAllows(
        mode: SessionMode,
        exercise: ExerciseDefinition,
        components: Map<String, KnowledgeComponent>
    ): Boolean {
        if (mode == SessionMode.ADAPTIVE) return true
        val domains = exercise.kcIds.mapNotNull { components[it]?.domain }.toSet()
        return when (mode) {
            SessionMode.VOCABULARY -> domains.any { it == KcDomain.VOCABULARY || it == KcDomain.CHUNK }
            SessionMode.GRAMMAR -> KcDomain.GRAMMAR in domains
            SessionMode.LISTENING -> KcDomain.LISTENING in domains || exercise.type == ExerciseType.LISTENING_AB
            SessionMode.SPELLING -> domains.any { it == KcDomain.ORTHOGRAPHY || it == KcDomain.LETTER } || exercise.type == ExerciseType.SPELLING_AB || exercise.type == ExerciseType.SPELL_FROM_AUDIO
            SessionMode.PRONUNCIATION -> KcDomain.PHONOLOGY in domains || exercise.type == ExerciseType.PRON_DISCRIMINATION
            SessionMode.ADAPTIVE -> true
        }
    }

    private fun fmt(v: Double) = "%.2f".format(java.util.Locale.US, v)
}

