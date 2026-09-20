package com.siaa.app

import android.content.Context
import com.siaa.core.model.SessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val maxItems: Int = 60,
    val announceControls: Boolean = true,
    val feedbackExplanations: Boolean = true,
    val defaultMode: SessionMode = SessionMode.ADAPTIVE,
    val speechRate: Float = 1.0f
)

class PreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("siaa_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    fun current(): UserPreferences = _state.value

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value).normalized()
        prefs.edit()
            .putInt(KEY_MAX_ITEMS, next.maxItems)
            .putBoolean(KEY_ANNOUNCE, next.announceControls)
            .putBoolean(KEY_EXPLANATIONS, next.feedbackExplanations)
            .putString(KEY_DEFAULT_MODE, next.defaultMode.name)
            .putFloat(KEY_SPEECH_RATE, next.speechRate)
            .apply()
        _state.value = next
    }

    private fun load(): UserPreferences = UserPreferences(
        maxItems = prefs.getInt(KEY_MAX_ITEMS, 60),
        announceControls = prefs.getBoolean(KEY_ANNOUNCE, true),
        feedbackExplanations = prefs.getBoolean(KEY_EXPLANATIONS, true),
        defaultMode = prefs.getString(KEY_DEFAULT_MODE, SessionMode.ADAPTIVE.name)
            ?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() }
            ?: SessionMode.ADAPTIVE,
        speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
    ).normalized()

    private fun UserPreferences.normalized() = copy(
        maxItems = maxItems.coerceIn(5, 300),
        speechRate = speechRate.coerceIn(0.5f, 2.0f)
    )

    companion object {
        private const val KEY_MAX_ITEMS = "max_items"
        private const val KEY_ANNOUNCE = "announce_controls"
        private const val KEY_EXPLANATIONS = "feedback_explanations"
        private const val KEY_DEFAULT_MODE = "default_mode"
        private const val KEY_SPEECH_RATE = "speech_rate"
    }
}
