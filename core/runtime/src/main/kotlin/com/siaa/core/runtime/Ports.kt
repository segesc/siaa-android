package com.siaa.core.runtime

import com.siaa.core.model.*

interface LearningRepository {
    suspend fun loadSnapshot(): LearningSnapshot
    suspend fun createSession(mode: SessionMode, nowEpochMs: Long): Long
    suspend fun finishSession(sessionId: Long, nowEpochMs: Long)
    suspend fun recordInteraction(record: InteractionRecord): Long
    suspend fun updateLearnerState(state: LearnerKcState)
    suspend fun recentInteractions(limit: Int = 80): List<InteractionRecord>
    suspend fun recentSessions(limit: Int = 20): List<SessionSummary> = emptyList()
    suspend fun dashboardStats(nowEpochMs: Long): DashboardStats
    suspend fun saveDeviceProfile(profile: DeviceProfile): Long
    suspend fun latestDeviceProfile(): DeviceProfile?
}

interface SpeechPort {
    suspend fun speak(text: String, languageTag: String = "es-PE", rate: Float = 1.0f)
    fun stop()
    fun shutdown()
}

interface EarconPort {
    fun play(kind: EarconKind)
    fun release()
}

fun interface ClockPort {
    fun nowEpochMs(): Long
}

object SystemClockPort : ClockPort {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
