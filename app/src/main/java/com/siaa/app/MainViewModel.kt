package com.siaa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siaa.core.model.DashboardStats
import com.siaa.core.model.DeviceProfile
import com.siaa.core.model.LearnerKcState
import com.siaa.core.model.KcDomain
import com.siaa.core.model.SessionPolicy
import com.siaa.core.model.SessionSummary
import com.siaa.core.algorithm.KnowledgeGraphEngine
import com.siaa.core.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = (application as SiaaApplication).graph
    private val _stats = MutableStateFlow<DashboardStats?>(null)
    private val _states = MutableStateFlow<List<LearnerKcState>>(emptyList())
    private val _deviceProfile = MutableStateFlow<DeviceProfile?>(null)
    private val _curriculum = MutableStateFlow<List<CurriculumItem>>(emptyList())
    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())

    private val mediaUi = combine(
        graph.mediaDiagnostics.lastEvent,
        graph.mediaDiagnostics.observedCommands,
        graph.preferenceStore.state
    ) { media, observed, preferences -> MediaUi(media, observed, preferences) }

    private val learnerUi = combine(_stats, _states, _deviceProfile, _curriculum, _sessions) { stats, states, profile, curriculum, sessions ->
        LearnerUi(stats, states, profile, curriculum, sessions)
    }

    val uiState: StateFlow<MainUiState> = combine(
        graph.runtime.snapshot,
        graph.contentReady,
        mediaUi,
        learnerUi
    ) { runtime, ready, media, learner ->
        MainUiState(
            runtime = runtime,
            contentReady = ready,
            lastMediaEvent = media.lastEvent,
            observedCommands = media.observedCommands,
            stats = learner.stats,
            states = learner.states,
            deviceProfile = learner.deviceProfile,
            curriculum = learner.curriculum,
            sessions = learner.sessions,
            preferences = media.preferences
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            graph.contentReady.collect { ready -> if (ready) refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!graph.contentReady.value) return@launch
            val now = System.currentTimeMillis()
            _stats.value = graph.repository.dashboardStats(now)
            val snapshot = graph.repository.loadSnapshot()
            _states.value = snapshot.states.sortedBy { it.kcId }
            val stateMap = snapshot.stateByKcId
            val kg = KnowledgeGraphEngine(snapshot.components, snapshot.edges)
            val policy = SessionPolicy()
            _curriculum.value = snapshot.components.map { kc ->
                CurriculumItem(
                    id = kc.id,
                    name = kc.name,
                    cefr = kc.cefr,
                    domain = kc.domain,
                    mastery = stateMap[kc.id]?.mastery ?: kc.priorMastery,
                    readiness = kg.readiness(kc.id, stateMap),
                    unlocked = kg.isUnlocked(kc.id, stateMap, now, policy)
                )
            }.sortedWith(compareBy<CurriculumItem> { cefrOrder(it.cefr) }.thenBy { it.domain.name }.thenBy { it.name })
            _deviceProfile.value = graph.repository.latestDeviceProfile()
            _sessions.value = graph.repository.recentSessions(20)
        }
    }


    fun setMaxItems(value: Int) = graph.preferenceStore.update { it.copy(maxItems = value) }
    fun setAnnounceControls(value: Boolean) = graph.preferenceStore.update { it.copy(announceControls = value) }
    fun setFeedbackExplanations(value: Boolean) = graph.preferenceStore.update { it.copy(feedbackExplanations = value) }

    private fun cefrOrder(level: String): Int = when (level.uppercase()) {
        "PRE-A1", "PREA1" -> 0
        "A1" -> 1
        "A2" -> 2
        "B1" -> 3
        "B2" -> 4
        "C1" -> 5
        "C2" -> 6
        else -> 99
    }

    fun clearCalibration() = graph.mediaDiagnostics.clear()

    fun saveCalibration(name: String = "Audífonos actuales") {
        viewModelScope.launch {
            val observed = graph.mediaDiagnostics.observedEvents.value
            val primary = observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.PLAY_PAUSE || it.event == com.siaa.core.runtime.MediaControlEvent.PLAY }
            val secondary = observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.NEXT }
            val back = observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.PREVIOUS }
            val stop = observed.lastOrNull { it.event == com.siaa.core.runtime.MediaControlEvent.STOP }

            val profile = DeviceProfile(
                name = name,
                primaryKeyCode = primary?.keyCode,
                secondaryKeyCode = secondary?.keyCode,
                backKeyCode = back?.keyCode,
                stopKeyCode = stop?.keyCode,
                playPauseAvailable = primary != null,
                nextAvailable = secondary != null,
                previousAvailable = back != null,
                lastSeenAtEpochMs = System.currentTimeMillis()
            )
            graph.repository.saveDeviceProfile(profile)
            _deviceProfile.value = graph.repository.latestDeviceProfile()
            _sessions.value = graph.repository.recentSessions(20)
        }
    }
}

private data class MediaUi(
    val lastEvent: String,
    val observedCommands: Set<String>,
    val preferences: UserPreferences
)

private data class LearnerUi(
    val stats: DashboardStats?,
    val states: List<LearnerKcState>,
    val deviceProfile: DeviceProfile?,
    val curriculum: List<CurriculumItem>,
    val sessions: List<SessionSummary>
)

data class MainUiState(
    val runtime: RuntimeSnapshot = RuntimeSnapshot(),
    val contentReady: Boolean = false,
    val lastMediaEvent: String = "Sin eventos",
    val observedCommands: Set<String> = emptySet(),
    val stats: DashboardStats? = null,
    val states: List<LearnerKcState> = emptyList(),
    val deviceProfile: DeviceProfile? = null,
    val curriculum: List<CurriculumItem> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)


data class CurriculumItem(
    val id: String,
    val name: String,
    val cefr: String,
    val domain: KcDomain,
    val mastery: Double,
    val readiness: Double,
    val unlocked: Boolean
)
