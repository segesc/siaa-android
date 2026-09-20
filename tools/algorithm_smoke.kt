import com.siaa.core.algorithm.*
import com.siaa.core.model.*

fun main() {
    val components = listOf(
        KnowledgeComponent("G_BASE", "Present simple", "A1", KcDomain.GRAMMAR, importance = 0.9),
        KnowledgeComponent("G_3SG", "Third person s", "A1", KcDomain.GRAMMAR, importance = 0.9)
    )
    val edges = listOf(KnowledgeEdge("G_BASE", "G_3SG"))
    val states = listOf(
        LearnerKcState("G_BASE", mastery = 0.90, totalAttempts = 8),
        LearnerKcState("G_3SG", mastery = 0.45, halfLifeHours = 12.0, totalAttempts = 3)
    )
    val exercises = listOf(
        ExerciseDefinition("E1", ExerciseType.AB, listOf("G_3SG"), "A1", 0.45, "Which?", optionA="She work", optionB="She works", correctOption="B"),
        ExerciseDefinition("E2", ExerciseType.TEACH, listOf("G_3SG"), "A1", 0.25, "Rule")
    )
    val snapshot = LearningSnapshot(components, edges, states, exercises)
    val planner = PomdpLookaheadPlanner()
    val ranked = planner.rank(SessionMode.GRAMMAR, snapshot, emptyList(), System.currentTimeMillis(), 5)
    check(ranked.isNotEmpty())
    println("top=${ranked.first().exercise.id} utility=${ranked.first().utility} ${ranked.first().rationale}")
    check(BktUpdater.posterior(0.4, true) > 0.4)
    check(kotlin.math.abs(HalfLifeModel.recallProbability(10.0, 10.0) - 0.5) < 1e-9)
    println("SIAA algorithm smoke OK")
}
