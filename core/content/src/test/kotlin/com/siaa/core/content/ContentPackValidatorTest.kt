package com.siaa.core.content

import com.siaa.core.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ContentPackValidatorTest {
    @Test fun detectsCycle() {
        val kcs = listOf(
            KnowledgeComponent("a", "A", "A1", KcDomain.GRAMMAR),
            KnowledgeComponent("b", "B", "A1", KcDomain.GRAMMAR)
        )
        val issues = ContentPackValidator.validate(kcs, listOf(KnowledgeEdge("a","b"), KnowledgeEdge("b","a")), emptyList())
        assertTrue(issues.any { it.code == "HARD_PREREQ_CYCLE" })
    }
}
