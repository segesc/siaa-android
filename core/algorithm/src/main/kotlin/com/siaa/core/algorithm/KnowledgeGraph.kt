package com.siaa.core.algorithm

import com.siaa.core.model.KnowledgeComponent
import com.siaa.core.model.KnowledgeEdge
import com.siaa.core.model.LearnerKcState

class KnowledgeGraphEngine(
    components: List<KnowledgeComponent>,
    private val edges: List<KnowledgeEdge>
) {
    private val ids = components.map { it.id }.toSet()
    private val incoming = edges.groupBy { it.toId }
    private val outgoing = edges.groupBy { it.fromId }

    fun readiness(kcId: String, states: Map<String, LearnerKcState>): Double {
        val prereqs = incoming[kcId].orEmpty().filter { it.fromId in ids }
        if (prereqs.isEmpty()) return 1.0
        val weighted = prereqs.map { edge ->
            val mastery = states[edge.fromId]?.mastery ?: 0.0
            mastery * edge.weight.coerceAtLeast(0.01)
        }
        val totalWeight = prereqs.sumOf { it.weight.coerceAtLeast(0.01) }
        return (weighted.sum() / totalWeight).coerceIn(0.0, 1.0)
    }

    fun isUnlocked(
        kcId: String,
        states: Map<String, LearnerKcState>,
        nowEpochMs: Long = System.currentTimeMillis(),
        policy: com.siaa.core.model.SessionPolicy = com.siaa.core.model.SessionPolicy()
    ): Boolean {
        val prereqs = incoming[kcId].orEmpty()
        if (prereqs.isEmpty()) return true
        return prereqs.all { edge ->
            if (!edge.hardPrerequisite) return@all true
            val prereqState = states[edge.fromId] ?: return@all false
            MasteryCheckpointEvaluator.evaluate(prereqState, nowEpochMs).passed &&
                prereqState.mastery >= policy.hardPrereqThreshold
        }
    }

    fun outerFringe(
        states: Map<String, LearnerKcState>,
        masteryThreshold: Double = 0.82,
        nowEpochMs: Long = System.currentTimeMillis(),
        policy: com.siaa.core.model.SessionPolicy = com.siaa.core.model.SessionPolicy()
    ): Set<String> = ids
        .filter { id -> (states[id]?.mastery ?: 0.0) < masteryThreshold && isUnlocked(id, states, nowEpochMs, policy) }
        .toSet()

    fun unlockValue(kcId: String, states: Map<String, LearnerKcState>): Double {
        val children = outgoing[kcId].orEmpty()
        if (children.isEmpty()) return 0.0
        return children.sumOf { edge ->
            val childMastery = states[edge.toId]?.mastery ?: 0.0
            val currentReadiness = readiness(edge.toId, states)
            (1.0 - childMastery) * (1.0 - currentReadiness) * edge.weight
        }.coerceAtLeast(0.0)
    }
}
