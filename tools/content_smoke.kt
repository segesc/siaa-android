import com.siaa.core.content.ContentPackValidator
import com.siaa.core.content.ControlledVariantGenerator
import com.siaa.core.content.LexemeSpec
import com.siaa.core.model.*

fun main() {
    val kcs = listOf(
        KnowledgeComponent("letter_e", "Letter E", "Pre-A1", KcDomain.LETTER),
        KnowledgeComponent("word_enough", "enough", "A2", KcDomain.VOCABULARY)
    )
    val edges = listOf(KnowledgeEdge("letter_e", "word_enough"))
    val exercises = listOf(
        ExerciseDefinition("e1", ExerciseType.TEACH, listOf("word_enough"), "A2", 0.3, "Nueva palabra", "enough")
    )
    val issues = ContentPackValidator.validate(kcs, edges, exercises)
    check(issues.none { it.severity.name == "ERROR" }) { issues }
    val generated = ControlledVariantGenerator().vocabularyVariants(
        LexemeSpec("word_enough", "enough", "suficiente", "A2", spellingDifficulty = 0.9, chunks = listOf("good enough"))
    )
    check(generated.size >= 3)
    println("SIAA content smoke OK variants=${generated.size}")
}
