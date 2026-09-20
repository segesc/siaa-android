package com.siaa.core.content

import java.util.ArrayDeque
import com.siaa.core.model.ExerciseDefinition
import com.siaa.core.model.KnowledgeComponent
import com.siaa.core.model.KnowledgeEdge

object ContentPackValidator {
    fun validate(
        components: List<KnowledgeComponent>,
        edges: List<KnowledgeEdge>,
        exercises: List<ExerciseDefinition>
    ): List<ContentValidationIssue> {
        val issues = mutableListOf<ContentValidationIssue>()
        duplicateIds(components.map { it.id }, "KC_DUPLICATE", issues)
        duplicateIds(exercises.map { it.id }, "EXERCISE_DUPLICATE", issues)
        val ids = components.map { it.id }.toSet()
        edges.forEach { edge ->
            if (edge.fromId !in ids) issues += error("EDGE_MISSING_FROM", "Prerequisito inexistente: ${edge.fromId}", edge.toId)
            if (edge.toId !in ids) issues += error("EDGE_MISSING_TO", "Destino inexistente: ${edge.toId}", edge.toId)
            if (edge.fromId == edge.toId) issues += error("EDGE_SELF_LOOP", "Un KC no puede ser prerrequisito de sí mismo", edge.toId)
        }
        exercises.forEach { ex ->
            if (ex.kcIds.isEmpty()) issues += error("EXERCISE_WITHOUT_KC", "Ejercicio sin KC", ex.id)
            ex.kcIds.filterNot { it in ids }.forEach { missing ->
                issues += error("EXERCISE_MISSING_KC", "El ejercicio referencia KC inexistente: $missing", ex.id)
            }
            if (ex.difficulty !in 0.0..1.0) issues += error("DIFFICULTY_RANGE", "Dificultad fuera de [0,1]", ex.id)
            if (ex.estimatedSeconds <= 0) issues += error("DURATION", "Duración estimada no positiva", ex.id)
        }
        issues += detectHardCycles(ids, edges.filter { it.hardPrerequisite })
        return issues
    }

    private fun duplicateIds(ids: List<String>, code: String, issues: MutableList<ContentValidationIssue>) {
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { id ->
            issues += error(code, "ID duplicado: $id", id)
        }
    }

    private fun detectHardCycles(ids: Set<String>, edges: List<KnowledgeEdge>): List<ContentValidationIssue> {
        val adjacency = edges.groupBy { it.fromId }.mapValues { (_, xs) -> xs.map { it.toId } }
        val color = mutableMapOf<String, Int>()
        val stack = ArrayDeque<String>()
        val issues = mutableListOf<ContentValidationIssue>()
        fun dfs(node: String) {
            color[node] = 1
            stack.addLast(node)
            for (next in adjacency[node].orEmpty()) {
                when (color[next] ?: 0) {
                    0 -> dfs(next)
                    1 -> {
                        val path = stack.dropWhile { it != next } + next
                        issues += error("HARD_PREREQ_CYCLE", "Ciclo de prerrequisitos: ${path.joinToString(" -> ")}", next)
                    }
                }
            }
            stack.removeLast()
            color[node] = 2
        }
        ids.forEach { if ((color[it] ?: 0) == 0) dfs(it) }
        return issues.distinctBy { it.message }
    }

    private fun error(code: String, message: String, id: String?) = ContentValidationIssue(
        ContentValidationIssue.Severity.ERROR, code, message, id
    )
}
