package com.siaa.core.runtime

import com.siaa.core.model.SessionMode

enum class LessonState {
    IDLE,
    PREPARING,
    SPEAKING,
    WAITING_BINARY,
    WAITING_SELF_ASSESSMENT,
    FEEDBACK,
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

data class SessionConfig(
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val maxItems: Int = 50,
    val announceControls: Boolean = true,
    val feedbackExplanations: Boolean = true
)

data class RuntimeSnapshot(
    val state: LessonState = LessonState.IDLE,
    val mode: SessionMode = SessionMode.ADAPTIVE,
    val sessionId: Long? = null,
    val currentExerciseId: String? = null,
    val currentKcId: String? = null,
    val message: String = "Listo",
    val completedItems: Int = 0,
    val correctItems: Int = 0,
    val promptFinishedAtMs: Long? = null,
    val helpDepth: Int = 0,
    val lastPlannerRationale: String = "",
    val error: String? = null
)
