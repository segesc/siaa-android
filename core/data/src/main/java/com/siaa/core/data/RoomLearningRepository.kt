package com.siaa.core.data

import androidx.room.withTransaction
import com.siaa.core.algorithm.HalfLifeModel
import com.siaa.core.algorithm.MasteryCheckpointEvaluator
import com.siaa.core.model.*
import com.siaa.core.runtime.LearningRepository

class RoomLearningRepository(private val db: SiaaDatabase) : LearningRepository {
    override suspend fun loadSnapshot(): LearningSnapshot {
        val components = db.contentDao().allKcs().map { it.toModel() }
        val states = db.learnerDao().allStates().map { it.toModel() }
        return LearningSnapshot(
            components = components,
            edges = db.contentDao().allEdges().map { it.toModel() },
            states = states,
            exercises = db.contentDao().allExercises().map { it.toModel() },
            misconceptions = db.learnerDao().allMisconceptions().map { it.toModel() }
        )
    }

    override suspend fun createSession(mode: SessionMode, nowEpochMs: Long): Long =
        db.sessionDao().insertSession(SessionEntity(mode = mode.name, startedAtEpochMs = nowEpochMs))

    override suspend fun finishSession(sessionId: Long, nowEpochMs: Long) =
        db.sessionDao().finishSession(sessionId, nowEpochMs)

    override suspend fun recordInteraction(record: InteractionRecord): Long =
        db.sessionDao().insertInteraction(record.toEntity())

    override suspend fun updateLearnerState(state: LearnerKcState) = db.learnerDao().upsertState(state.toEntity())

    override suspend fun commitTurn(
        interaction: InteractionRecord,
        updatedStates: List<LearnerKcState>,
        misconceptionUpdates: List<Misconception>
    ): Long = db.withTransaction {
        updatedStates.forEach { db.learnerDao().upsertState(it.toEntity()) }
        misconceptionUpdates.forEach { db.learnerDao().upsertMisconception(it.toEntity()) }
        db.sessionDao().insertInteraction(interaction.toEntity())
    }

    override suspend fun recordRuntimeEvent(event: RuntimeEvent): Long =
        db.sessionDao().insertRuntimeEvent(event.toEntity())

    override suspend fun recentRuntimeEvents(limit: Int): List<RuntimeEvent> =
        db.sessionDao().recentRuntimeEvents(limit).map { it.toModel() }

    override suspend fun recentInteractions(limit: Int): List<InteractionRecord> =
        db.sessionDao().recentInteractions(limit).map { it.toModel() }

    override suspend fun recentSessions(limit: Int): List<SessionSummary> =
        db.sessionDao().recentSessions(limit).map { row ->
            SessionSummary(
                id = row.id,
                mode = runCatching { SessionMode.valueOf(row.mode) }.getOrDefault(SessionMode.ADAPTIVE),
                startedAtEpochMs = row.startedAtEpochMs,
                endedAtEpochMs = row.endedAtEpochMs,
                completedItems = row.completedItems,
                correctItems = row.correctItems,
                meanLatencyMs = row.meanLatencyMs
            )
        }

    override suspend fun dashboardStats(nowEpochMs: Long): DashboardStats {
        val snapshot = loadSnapshot()
        val states = snapshot.states
        val total = snapshot.components.size
        val mastered = states.count { s ->
            MasteryCheckpointEvaluator.evaluate(s, nowEpochMs).passed
        }
        val due = states.count { s ->
            val elapsed = s.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: Double.POSITIVE_INFINITY
            val recall = if (elapsed.isFinite()) HalfLifeModel.recallProbability(elapsed, s.halfLifeHours) else 0.0
            recall < 0.75 && s.totalAttempts > 0
        }
        val avgMastery = if (states.isEmpty()) 0.0 else states.map { it.mastery }.average()
        val avgRetention = if (states.isEmpty()) 0.0 else states.map { s ->
            val elapsed = s.lastReviewedAtEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) / 3_600_000.0 } ?: Double.POSITIVE_INFINITY
            if (elapsed.isFinite()) HalfLifeModel.recallProbability(elapsed, s.halfLifeHours) else 0.0
        }.average()
        val cefr = estimateCefr(snapshot, nowEpochMs)
        return DashboardStats(total, mastered, due, avgMastery, avgRetention, db.sessionDao().interactionCount(), cefr)
    }

    override suspend fun saveDeviceProfile(profile: DeviceProfile): Long = db.deviceDao().save(profile.toEntity())
    override suspend fun latestDeviceProfile(): DeviceProfile? = db.deviceDao().latest()?.toModel()

    private fun estimateCefr(snapshot: LearningSnapshot, nowEpochMs: Long): String {
        val order = listOf("Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2")
        var best = "Pre-A1"
        for (level in order) {
            val kcs = snapshot.components.filter { it.cefr.equals(level, true) }
            if (kcs.isEmpty()) continue
            val passedCount = kcs.count { kc ->
                val state = snapshot.stateByKcId[kc.id] ?: return@count false
                MasteryCheckpointEvaluator.evaluate(state, nowEpochMs).passed
            }
            val coverage = passedCount.toDouble() / kcs.size
            if (coverage >= 0.70) {
                best = level
            } else {
                break
            }
        }
        return "Progreso CEFR estimado por SIAA: $best"
    }
}

