package com.siaa.core.data

import androidx.room.*

@Dao
interface ContentDao {
    @Query("SELECT * FROM knowledge_components") suspend fun allKcs(): List<KnowledgeComponentEntity>
    @Query("SELECT * FROM knowledge_edges") suspend fun allEdges(): List<KnowledgeEdgeEntity>
    @Query("SELECT * FROM exercises") suspend fun allExercises(): List<ExerciseEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertKcs(items: List<KnowledgeComponentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEdges(items: List<KnowledgeEdgeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercises(items: List<ExerciseEntity>)
    @Query("SELECT COUNT(*) FROM knowledge_components") suspend fun kcCount(): Int
    @Query("DELETE FROM knowledge_edges") suspend fun clearEdges()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM knowledge_components") suspend fun clearKcs()
}

@Dao
interface LearnerDao {
    @Query("SELECT * FROM learner_kc_state") suspend fun allStates(): List<LearnerKcStateEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertState(state: LearnerKcStateEntity)
    @Query("DELETE FROM learner_kc_state WHERE kcId = :kcId") suspend fun deleteState(kcId: String)
    @Query("DELETE FROM learner_kc_state WHERE kcId NOT IN (:validKcIds)") suspend fun deleteOrphanStates(validKcIds: List<String>)
    @Query("SELECT * FROM misconceptions") suspend fun allMisconceptions(): List<MisconceptionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMisconception(item: MisconceptionEntity)
}


data class SessionSummaryRow(
    val id: Long,
    val mode: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val completedItems: Int,
    val correctItems: Int,
    val meanLatencyMs: Double?
)

@Dao
interface SessionDao {
    @Insert suspend fun insertSession(session: SessionEntity): Long
    @Query("UPDATE sessions SET endedAtEpochMs=:endedAt WHERE id=:id") suspend fun finishSession(id: Long, endedAt: Long)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertInteraction(interaction: InteractionEntity): Long
    @Query("SELECT * FROM interactions ORDER BY timestampEpochMs DESC LIMIT :limit") suspend fun recentInteractions(limit: Int): List<InteractionEntity>
    @Query("SELECT COUNT(*) FROM interactions WHERE graded = 1") suspend fun interactionCount(): Int
    @Query("""
        SELECT s.id AS id, s.mode AS mode, s.startedAtEpochMs AS startedAtEpochMs, s.endedAtEpochMs AS endedAtEpochMs,
               COALESCE(SUM(CASE WHEN i.graded = 1 THEN 1 ELSE 0 END), 0) AS completedItems,
               COALESCE(SUM(CASE WHEN i.graded = 1 AND i.correct = 1 THEN 1 ELSE 0 END), 0) AS correctItems,
               AVG(CASE WHEN i.graded = 1 THEN i.latencyMs ELSE NULL END) AS meanLatencyMs
        FROM sessions s
        LEFT JOIN interactions i ON i.sessionId = s.id
        GROUP BY s.id
        ORDER BY s.startedAtEpochMs DESC
        LIMIT :limit
    """)
    suspend fun recentSessions(limit: Int): List<SessionSummaryRow>

    @Insert suspend fun insertRuntimeEvent(event: RuntimeEventEntity): Long
    @Query("SELECT * FROM runtime_events ORDER BY timestampEpochMs DESC LIMIT :limit") suspend fun recentRuntimeEvents(limit: Int): List<RuntimeEventEntity>
}


@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(profile: DeviceProfileEntity): Long
    @Query("SELECT * FROM device_profiles ORDER BY lastSeenAtEpochMs DESC LIMIT 1") suspend fun latest(): DeviceProfileEntity?
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM app_meta WHERE `key`=:key LIMIT 1") suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(item: AppMetaEntity)
}
