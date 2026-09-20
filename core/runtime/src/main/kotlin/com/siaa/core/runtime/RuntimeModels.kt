package com.siaa.core.runtime

import com.siaa.core.model.SessionCapabilities
import com.siaa.core.model.SessionMode
import com.siaa.core.model.SessionPolicy

enum class LessonState {
    IDLE,
    PREPARING,
    SPEAKING,
    WAITING_BINARY,
    WAITING_SELF_ASSESSMENT,
    EVALUATING,
    FEEDBACK,
    HELPING,
    PLANNING_NEXT,
    PAUSED,
    SESSION_END,
    ERROR
}

enum class RuntimeCommand {
    PRIMARY,
    SECONDARY,
    BACK,
    PAUSE,
    PLAY,
    STOP
}

enum class PauseReason {
    USER_REQUESTED,
    AUDIO_ROUTE_LOST,
    AUDIO_FOCUS_LOST
}

data class TurnContext(
    val turnId: Long = 0L,
    val exerciseId: String = "",
    val expectedState: LessonState = LessonState.IDLE,
    val helpDepth: Int = 0,
    val promptVariant: String = "",
    val promptFinishedAtEpochMs: Long = 0L,
    val timeoutRetries: Int = 0
)

data class SessionConfig(
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val maxItems: Int = 50,
    val announceControls: Boolean = true,
    val feedbackExplanations: Boolean = true,
    val capabilities: SessionCapabilities = SessionCapabilities(),
    val policy: SessionPolicy = SessionPolicy()
)

data class RuntimeSnapshot(
    val state: LessonState = LessonState.IDLE,
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val sessionId: Long? = null,
    val turnId: Long = 0L,
    val currentExerciseId: String? = null,
    val currentKcId: String? = null,
    val message: String = "Listo",
    val completedItems: Int = 0,
    val correctItems: Int = 0,
    val promptFinishedAtMs: Long? = null,
    val helpDepth: Int = 0,
    val lastPlannerRationale: String = "",
    val pausedFrom: LessonState? = null,
    val pauseReason: PauseReason? = null,
    val error: String? = null
)

