package com.siaa.core.runtime

import com.siaa.core.algorithm.*
import com.siaa.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonRuntimeUnitTest {

    private class TestRepo : LearningRepository {
        val comps = listOf(KnowledgeComponent("G", "3sg", "A1", KcDomain.GRAMMAR, priorMastery = 0.2))
        val exs = listOf(
            ExerciseDefinition("E1", ExerciseType.AB, listOf("G"), "A1", .4, "Cuál?", optionA = "she work", optionB = "she works", correctOption = "B", explanationEs = "Con she usamos works"),
            ExerciseDefinition("E2", ExerciseType.SELF_ASSESS, listOf("G"), "A1", .5, "Construye mentalmente: ella trabaja", explanationEs = "She works")
        )
        val states = mutableMapOf<String, LearnerKcState>()
        val interactions = mutableListOf<InteractionRecord>()
        val events = mutableListOf<RuntimeEvent>()

        override suspend fun loadSnapshot() = LearningSnapshot(comps, emptyList(), states.values.toList(), exs)
        override suspend fun createSession(mode: SessionMode, nowEpochMs: Long) = 1L
        override suspend fun finishSession(sessionId: Long, nowEpochMs: Long) {}
        override suspend fun recordInteraction(record: InteractionRecord): Long { interactions += record; return interactions.size.toLong() }
        override suspend fun updateLearnerState(state: LearnerKcState) { states[state.kcId] = state }
        override suspend fun commitTurn(
            interaction: InteractionRecord,
            updatedStates: List<LearnerKcState>,
            misconceptionUpdates: List<Misconception>
        ): Long {
            updatedStates.forEach { states[it.kcId] = it }
            interactions += interaction
            return interactions.size.toLong()
        }
        override suspend fun recordRuntimeEvent(event: RuntimeEvent): Long {
            events += event
            return events.size.toLong()
        }
        override suspend fun recentRuntimeEvents(limit: Int) = events.asReversed().take(limit)
        override suspend fun recentInteractions(limit: Int) = interactions.asReversed().take(limit)
        override suspend fun dashboardStats(nowEpochMs: Long) = DashboardStats(1, 0, 0, 0.0, 0.0, interactions.size, "A1")
        override suspend fun saveDeviceProfile(profile: DeviceProfile) = 1L
        override suspend fun latestDeviceProfile(): DeviceProfile? = null
    }

    private class TestSpeech : SpeechPort {
        val spoken = mutableListOf<String>()
        override suspend fun speak(text: String, languageTag: String, rate: Float) { spoken += text }
        override fun stop() {}
        override fun shutdown() {}
    }

    private class TestEarcon : EarconPort {
        val played = mutableListOf<EarconKind>()
        override fun play(kind: EarconKind) { played += kind }
        override fun release() {}
    }

    private class TestPlanner : ExercisePlanner {
        override fun rank(
            mode: SessionMode,
            snapshot: LearningSnapshot,
            recentInteractions: List<InteractionRecord>,
            nowEpochMs: Long,
            limit: Int
        ): List<PlannerCandidate> {
            val seen = recentInteractions.map { it.exerciseId }.toSet()
            val e = snapshot.exercises.firstOrNull { it.id !in seen } ?: return emptyList()
            return listOf(PlannerCandidate(e, .5, .6, .5, .5, 0.0, 0.0, "unit_test"))
        }
    }

    private suspend fun waitFor(runtime: LessonRuntime, targetState: LessonState) {
        repeat(300) {
            if (runtime.snapshot.value.state == targetState) return
            delay(10)
        }
        error("Timeout waiting for $targetState. Actual snapshot: ${runtime.snapshot.value}")
    }

    @Test
    fun testFullLessonTurnExecutionAndPersistence() = runBlocking {
        val repo = TestRepo()
        val speech = TestSpeech()
        val ear = TestEarcon()
        val planner = TestPlanner()
        val runtime = LessonRuntime(
            repository = repo,
            planner = planner,
            stateUpdater = StateUpdater(),
            speech = speech,
            earcon = ear,
            dispatcher = Dispatchers.Default
        )

        runtime.start(SessionConfig(SessionMode.GRAMMAR, maxItems = 2, announceControls = false))
        waitFor(runtime, LessonState.WAITING_BINARY)

        assertTrue(runtime.onCommand(RuntimeCommand.SECONDARY))
        waitFor(runtime, LessonState.WAITING_SELF_ASSESSMENT)

        assertTrue(runtime.onCommand(RuntimeCommand.PRIMARY))
        waitFor(runtime, LessonState.SESSION_END)

        assertEquals(2, repo.interactions.size)
        assertTrue(repo.states.containsKey("G"))
        assertTrue(repo.states["G"]!!.mastery > 0.2)
        assertTrue(repo.events.isNotEmpty())

        runtime.shutdown()
    }
}
