package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.math.abs
import kotlin.math.exp

class AdaptiveUtilityPlanner(
    private val onlineModels: OnlineAdaptiveModels = OnlineAdaptiveModels(),
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic()
) : ExercisePlanner {
    override fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int
    ): List<PlannerCandidate> {
        val states = snapshot.states.associateBy { it.kcId }
        val graph = KnowledgeGraphEngine(snapshot.components, snapshot.edges)
        val recentIds = recentInteractions.take(8).map { it.exerciseId }.toSet()
        val eligible = snapshot.exercises.asSequence()
            .filter { modeAllows(mode, it, snapshot.componentById) }
            .filter { exercise -> exercise.kcIds.all { graph.isUnlocked(it, states) || exercise.type == ExerciseType.TEACH } }
            .filterNot { it.id in recentIds && snapshot.exercises.size > 12 }
            .map { exercise -> score(exercise, snapshot, states, graph, recentInteractions, nowEpochMs) }
            .sortedByDescending { it.utility }
            .take(limit)
            .toList()
        return eligible.ifEmpty {
            snapshot.exercises.asSequence()
                .filter { modeAllows(mode, it, snapshot.componentById) }
                .map { score(it, snapshot, states, graph, recentInteractions, nowEpochMs) }
                .sortedByDescending { it.utility }
                .take(limit)
                .toList()
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
        val primaryKc = exercise.kcIds.firstOrNull()
        val component = primaryKc?.let(snapshot.componentById::get)
        val state = primaryKc?.let(states::get) ?: LearnerKcState(primaryKc ?: "unknown")
        val elapsedHours = state.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: 9999.0
        val recall = if (state.lastReviewedAtEpochMs == null) 0.35 else onlineModels.recallProbability(primaryKc ?: "unknown", elapsedHours, state.halfLifeHours)
        val retentionUrgency = (1.0 - recall).coerceIn(0.0, 1.0)
        val masteryGap = (1.0 - state.mastery).coerceIn(0.0, 1.0)
        val autoGap = (1.0 - state.automaticity).coerceIn(0.0, 1.0)
        val info = MathUtils.binaryEntropy(state.mastery) * state.uncertainty.coerceAtLeast(0.15)
        val successP = diagnostic.predictedCorrect(exercise, states)
        val targetDifficulty = (0.25 + 0.65 * state.mastery).coerceIn(0.25, 0.90)
        val difficultyMatch = exp(-2.2 * abs(exercise.difficulty - targetDifficulty))
        val unlock = primaryKc?.let { graph.unlockValue(it, states) } ?: 0.0
        val importance = component?.importance ?: 0.5
        val readiness = primaryKc?.let { graph.readiness(it, states) } ?: 1.0
        val durationPenalty = (exercise.estimatedSeconds / 90.0).coerceIn(0.0, 1.0)
        val riskPenalty = when {
            successP < 0.25 && exercise.type != ExerciseType.TEACH -> 0.9
            successP < 0.40 -> 0.35
            successP > 0.96 && masteryGap < 0.12 -> 0.25
            else -> 0.0
        }
        val teachingBoost = if (exercise.type == ExerciseType.TEACH && state.totalAttempts == 0) 0.55 else 0.0
        val kcExerciseIds = primaryKc?.let { kc -> snapshot.exercises.filter { kc in it.kcIds }.map { it.id }.toSet() }.orEmpty()
        val wheel = WheelSpinningDetector.detect(kcExerciseIds, recentInteractions)
        val remediationBoost = when {
            !wheel.detected -> 0.0
            exercise.type == ExerciseType.TEACH -> 0.70
            "remedial" in exercise.tags || "contrast" in exercise.tags -> 0.35
            else -> -0.45
        }
        val strategySample = primaryKc?.let { onlineModels.strategySample(it, exercise.type) } ?: 0.5
        val explorationBonus = 0.06 * (strategySample - 0.5)
        val utility = (
            0.23 * retentionUrgency +
            0.19 * masteryGap +
            0.10 * autoGap +
            0.15 * info +
            0.08 * unlock.coerceIn(0.0, 1.0) +
            0.10 * difficultyMatch +
            0.08 * importance +
            0.07 * readiness +
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
            retentionUrgency = retentionUrgency,
            informationValue = info,
            unlockValue = unlock,
            riskPenalty = riskPenalty,
            rationale = "mastery=${fmt(state.mastery)}, recall=${fmt(recall)}, readiness=${fmt(readiness)}, info=${fmt(info)}, wheel=${wheel.detected}, bandit=${fmt(strategySample)}"
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
