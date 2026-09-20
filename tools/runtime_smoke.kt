import com.siaa.core.algorithm.*
import com.siaa.core.model.*
import com.siaa.core.runtime.*
import kotlinx.coroutines.*

class Repo : LearningRepository {
    val comps = listOf(KnowledgeComponent("G", "3sg", "A1", KcDomain.GRAMMAR, priorMastery = 0.2))
    val exs = listOf(
        ExerciseDefinition("E1", ExerciseType.AB, listOf("G"), "A1", .4, "Cuál?", optionA="she work", optionB="she works", correctOption="B", explanationEs="Con she usamos works"),
        ExerciseDefinition("E2", ExerciseType.SELF_ASSESS, listOf("G"), "A1", .5, "Construye mentalmente: ella trabaja", explanationEs="She works")
    )
    var states = mutableMapOf<String,LearnerKcState>()
    val interactions = mutableListOf<InteractionRecord>()
    override suspend fun loadSnapshot() = LearningSnapshot(comps, emptyList(), states.values.toList(), exs)
    override suspend fun createSession(mode: SessionMode, nowEpochMs: Long) = 1L
    override suspend fun finishSession(sessionId: Long, nowEpochMs: Long) {}
    override suspend fun recordInteraction(record: InteractionRecord): Long { interactions += record; return interactions.size.toLong() }
    override suspend fun updateLearnerState(state: LearnerKcState) { states[state.kcId]=state }
    override suspend fun recentInteractions(limit: Int) = interactions.asReversed().take(limit)
    override suspend fun dashboardStats(nowEpochMs: Long) = DashboardStats(1,0,0,0.0,0.0,interactions.size,"A1")
    override suspend fun saveDeviceProfile(profile: DeviceProfile)=1L
    override suspend fun latestDeviceProfile(): DeviceProfile?=null
}
class Speech: SpeechPort { override suspend fun speak(text:String,languageTag:String,rate:Float){ println("SPEAK[$languageTag@$rate] $text") }; override fun stop(){}; override fun shutdown(){} }
class Ear: EarconPort { override fun play(kind:EarconKind){ println("EAR $kind") }; override fun release(){} }
class P: ExercisePlanner {
    override fun rank(mode:SessionMode,snapshot:LearningSnapshot,recentInteractions:List<InteractionRecord>,nowEpochMs:Long,limit:Int):List<PlannerCandidate>{
        val seen=recentInteractions.map{it.exerciseId}.toSet(); val e=snapshot.exercises.firstOrNull{it.id !in seen}?:return emptyList()
        return listOf(PlannerCandidate(e,.5,.6,.5,.5,0.0,0.0,"smoke"))
    }
}

suspend fun waitFor(runtime: LessonRuntime, state: LessonState) {
    repeat(300) { if (runtime.snapshot.value.state==state) return; delay(10) }
    error("timeout waiting $state; got ${runtime.snapshot.value}")
}
fun main()= runBlocking {
    val repo=Repo(); val runtime=LessonRuntime(repo,P(),StateUpdater(),Speech(),Ear(), dispatcher=Dispatchers.Default)
    runtime.start(SessionConfig(SessionMode.GRAMMAR,maxItems=2,announceControls=false))
    waitFor(runtime,LessonState.WAITING_BINARY)
    check(runtime.onCommand(RuntimeCommand.SECONDARY))
    waitFor(runtime,LessonState.WAITING_SELF_ASSESSMENT)
    check(runtime.onCommand(RuntimeCommand.PRIMARY))
    waitFor(runtime,LessonState.SESSION_END)
    check(repo.interactions.size==2)
    check(repo.states["G"]!!.mastery>0.2)
    println("SIAA runtime smoke OK mastery=${repo.states["G"]!!.mastery}")
    runtime.shutdown()
}
