package com.siaa.core.algorithm

import com.siaa.core.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class AlgorithmTest {
    @Test fun bktCorrectRaisesMastery() {
        val before = 0.35
        val after = BktUpdater.posterior(before, true)
        assertTrue(after > before)
    }

    @Test fun halfLifeDecaysToHalf() {
        val p = HalfLifeModel.recallProbability(12.0, 12.0)
        assertTrue(kotlin.math.abs(p - 0.5) < 1e-9)
    }

    @Test fun prerequisiteLocksChild() {
        val comps = listOf(
            KnowledgeComponent("A", "A", "A1", KcDomain.GRAMMAR),
            KnowledgeComponent("B", "B", "A1", KcDomain.GRAMMAR)
        )
        val graph = KnowledgeGraphEngine(comps, listOf(KnowledgeEdge("A", "B")))
        val states = mapOf("A" to LearnerKcState("A", mastery = 0.20))
        assertTrue(!graph.isUnlocked("B", states))
    }

    @Test fun plannerRespectsSessionCapabilities() {
        val comps = listOf(KnowledgeComponent("G", "Grammar", "A1", KcDomain.GRAMMAR))
        val exercises = listOf(
            ExerciseDefinition("E_BINARY", ExerciseType.AB, listOf("G"), "A1", 0.4, "Pregunta A/B"),
            ExerciseDefinition("E_SELF", ExerciseType.SELF_ASSESS, listOf("G"), "A1", 0.5, "Autoevaluación")
        )
        val snapshot = LearningSnapshot(comps, emptyList(), listOf(LearnerKcState("G", mastery = 0.5)), exercises)
        val planner = AdaptiveUtilityPlanner()

        // With default capabilities (3-way available), self-assess is allowed
        val fullRank = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), 0L, 10, SessionCapabilities(hasBack = true))
        assertTrue(fullRank.any { it.exercise.id == "E_SELF" })

        // Without back button, 3-way self-assessment cannot be presented
        val restrictedRank = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), 0L, 10, SessionCapabilities(hasBack = false))
        assertTrue(restrictedRank.none { it.exercise.id == "E_SELF" })
        assertTrue(restrictedRank.any { it.exercise.id == "E_BINARY" })
    }
}
