package com.siaa.core.model

enum class KcDomain {
    GRAMMAR, VOCABULARY, ORTHOGRAPHY, LISTENING, PHONOLOGY, PRAGMATICS, CHUNK, LETTER
}

enum class SessionMode {
    ADAPTIVE, VOCABULARY, GRAMMAR, LISTENING, SPELLING, PRONUNCIATION
}

enum class ExerciseType {
    TEACH,
    AB,
    SELF_ASSESS,
    LISTENING_AB,
    SPELLING_AB,
    SPELL_FROM_AUDIO,
    MEANING_AB,
    CHUNK_AB,
    PRON_DISCRIMINATION
}

enum class EarconKind { CORRECT, INCORRECT, REGISTERED, NEW_PROMPT, WARNING }

enum class ResponseConfidence { CORRECT, UNSURE, WRONG }

data class KnowledgeComponent(
    val id: String,
    val name: String,
    val cefr: String,
    val domain: KcDomain,
    val form: String = "",
    val meaning: String = "",
    val use: String = "",
    val importance: Double = 0.5,
    val priorMastery: Double = 0.15,
    val tags: Set<String> = emptySet()
)

data class KnowledgeEdge(
    val fromId: String,
    val toId: String,
    val weight: Double = 1.0,
    val hardPrerequisite: Boolean = true
)

data class LearnerKcState(
    val kcId: String,
    val mastery: Double = 0.15,
    val recognition: Double = 0.15,
    val production: Double = 0.10,
    val orthography: Double = 0.10,
    val automaticity: Double = 0.05,
    val halfLifeHours: Double = 8.0,
    val uncertainty: Double = 0.45,
    val lastReviewedAtEpochMs: Long? = null,
    val consecutiveSuccess: Int = 0,
    val consecutiveFailure: Int = 0,
    val totalAttempts: Int = 0,
    val totalCorrect: Int = 0
)

data class ExerciseDefinition(
    val id: String,
    val type: ExerciseType,
    val kcIds: List<String>,
    val cefr: String,
    val difficulty: Double,
    val promptEs: String,
    val stimulusEn: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val correctOption: String = "",
    val explanationEs: String = "",
    val spellTarget: String = "",
    val estimatedSeconds: Int = 20,
    val tags: Set<String> = emptySet()
)

data class InteractionRecord(
    val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: String,
    val timestampEpochMs: Long,
    val response: String,
    val correct: Boolean,
    val confidence: ResponseConfidence? = null,
    val latencyMs: Long? = null,
    val hintDepth: Int = 0,
    val plannerScore: Double? = null,
    val stateBeforeMastery: Double? = null,
    val stateAfterMastery: Double? = null
)

data class SessionRecord(
    val id: Long = 0L,
    val mode: SessionMode,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val completedItems: Int = 0,
    val correctItems: Int = 0
)

data class SessionSummary(
    val id: Long,
    val mode: SessionMode,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val completedItems: Int,
    val correctItems: Int,
    val meanLatencyMs: Double?
) {
    val accuracy: Double get() = if (completedItems == 0) 0.0 else correctItems.toDouble() / completedItems
}

data class DeviceProfile(
    val id: Long = 0L,
    val name: String,
    val playPauseAvailable: Boolean = true,
    val nextAvailable: Boolean = true,
    val previousAvailable: Boolean = true,
    val lastSeenAtEpochMs: Long = 0L
)

data class Misconception(
    val id: String,
    val kcId: String,
    val label: String,
    val probability: Double,
    val lastObservedAtEpochMs: Long
)

data class LearningSnapshot(
    val components: List<KnowledgeComponent>,
    val edges: List<KnowledgeEdge>,
    val states: List<LearnerKcState>,
    val exercises: List<ExerciseDefinition>,
    val misconceptions: List<Misconception> = emptyList()
) {
    val componentById: Map<String, KnowledgeComponent> by lazy { components.associateBy { it.id } }
    val stateByKcId: Map<String, LearnerKcState> by lazy { states.associateBy { it.kcId } }
    val exerciseById: Map<String, ExerciseDefinition> by lazy { exercises.associateBy { it.id } }
}

data class PlannerCandidate(
    val exercise: ExerciseDefinition,
    val utility: Double,
    val successProbability: Double,
    val retentionUrgency: Double,
    val informationValue: Double,
    val unlockValue: Double,
    val riskPenalty: Double,
    val rationale: String
)

data class DashboardStats(
    val totalKcs: Int,
    val masteredKcs: Int,
    val dueKcs: Int,
    val averageMastery: Double,
    val averageRetention: Double,
    val totalInteractions: Int,
    val currentCefrEstimate: String
)
