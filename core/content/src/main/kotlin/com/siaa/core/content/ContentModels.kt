package com.siaa.core.content

import com.siaa.core.model.KcDomain

data class LexemeSpec(
    val id: String,
    val lemma: String,
    val meaningEs: String,
    val cefr: String,
    val frequencyRank: Int? = null,
    val spellingDifficulty: Double = 0.25,
    val distractorEs: String = "otra opción",
    val chunks: List<String> = emptyList(),
    val exampleFrames: List<String> = emptyList(),
    val tags: Set<String> = emptySet()
)

data class GrammarPatternSpec(
    val id: String,
    val name: String,
    val cefr: String,
    val form: String,
    val meaning: String,
    val use: String,
    val prerequisites: Set<String> = emptySet(),
    val examples: List<String> = emptyList(),
    val contrasts: List<String> = emptyList(),
    val commonErrors: List<String> = emptyList()
)

data class ContentPackManifest(
    val id: String,
    val version: String,
    val localeL1: String = "es-PE",
    val localeL2: String = "en-US",
    val title: String,
    val sourceNotes: List<String> = emptyList()
)

data class ContentValidationIssue(
    val severity: Severity,
    val code: String,
    val message: String,
    val entityId: String? = null
) {
    enum class Severity { WARNING, ERROR }
}

data class GeneratedExerciseSpec(
    val id: String,
    val kcId: String,
    val domain: KcDomain,
    val kind: String,
    val promptEs: String,
    val stimulusEn: String,
    val optionA: String = "",
    val optionB: String = "",
    val correctOption: String = "",
    val explanationEs: String = "",
    val spellTarget: String = ""
)
