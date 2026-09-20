package com.siaa.core.algorithm

import com.siaa.core.model.ExerciseDefinition
import com.siaa.core.model.InteractionRecord
import com.siaa.core.model.LearningSnapshot
import com.siaa.core.model.PlannerCandidate
import com.siaa.core.model.SessionCapabilities
import com.siaa.core.model.SessionMode

interface ExercisePlanner {
    fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int = 12
    ): List<PlannerCandidate> = rank(mode, snapshot, recentInteractions, nowEpochMs, limit, SessionCapabilities())

    fun rank(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        limit: Int,
        capabilities: SessionCapabilities
    ): List<PlannerCandidate> = rank(mode, snapshot, recentInteractions, nowEpochMs, limit)

    fun choose(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long
    ): PlannerCandidate? = rank(mode, snapshot, recentInteractions, nowEpochMs, 1).firstOrNull()

    fun choose(
        mode: SessionMode,
        snapshot: LearningSnapshot,
        recentInteractions: List<InteractionRecord>,
        nowEpochMs: Long,
        capabilities: SessionCapabilities
    ): PlannerCandidate? = rank(mode, snapshot, recentInteractions, nowEpochMs, 1, capabilities).firstOrNull()
}
