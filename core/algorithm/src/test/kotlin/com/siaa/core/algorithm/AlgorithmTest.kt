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
}
