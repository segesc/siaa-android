package com.siaa.core.content

import com.siaa.core.model.KcDomain
import java.util.Locale

/**
 * Generador determinista y restringido de variantes léxicas. No usa un LLM en runtime:
 * todas las salidas derivan de datos curados, por lo que son auditables y reproducibles.
 */
class ControlledVariantGenerator {
    fun vocabularyVariants(lexeme: LexemeSpec): List<GeneratedExerciseSpec> {
        val safe = idSafe(lexeme.id)
        val distractor = lexeme.distractorEs.ifBlank { "otra opción" }
        val meaningCorrectA = stableCorrectA(lexeme.id + "meaning")
        val meaningA = if (meaningCorrectA) lexeme.meaningEs else distractor
        val meaningB = if (meaningCorrectA) distractor else lexeme.meaningEs
        val out = mutableListOf<GeneratedExerciseSpec>()
        out += GeneratedExerciseSpec(
            id = "gen_${safe}_meaning",
            kcId = lexeme.id,
            domain = KcDomain.VOCABULARY,
            kind = "MEANING_AB",
            promptEs = "¿Qué significa ${lexeme.lemma}?",
            stimulusEn = lexeme.lemma,
            optionA = meaningA,
            optionB = meaningB,
            correctOption = if (meaningCorrectA) "A" else "B",
            explanationEs = "${lexeme.lemma} significa ${lexeme.meaningEs}."
        )
        out += GeneratedExerciseSpec(
            id = "gen_${safe}_spell_recall",
            kcId = lexeme.id,
            domain = KcDomain.ORTHOGRAPHY,
            kind = "SPELL_FROM_AUDIO",
            promptEs = "Escucha la palabra y forma mentalmente su deletreo.",
            stimulusEn = lexeme.lemma,
            explanationEs = "La forma escrita es ${lexeme.lemma}.",
            spellTarget = lexeme.lemma
        )
        val misspelling = plausibleMisspelling(lexeme.lemma)
        val spellCorrectA = stableCorrectA(lexeme.id + "spelling")
        out += GeneratedExerciseSpec(
            id = "gen_${safe}_spell_ab",
            kcId = lexeme.id,
            domain = KcDomain.ORTHOGRAPHY,
            kind = "SPELLING_AB",
            promptEs = "¿Cuál de estas formas corresponde a la palabra que escuchaste?",
            stimulusEn = lexeme.lemma,
            optionA = if (spellCorrectA) lexeme.lemma else misspelling,
            optionB = if (spellCorrectA) misspelling else lexeme.lemma,
            correctOption = if (spellCorrectA) "A" else "B",
            explanationEs = "La forma escrita correcta es ${lexeme.lemma}.",
            spellTarget = lexeme.lemma
        )
        lexeme.chunks.take(3).forEachIndexed { index, chunk ->
            out += GeneratedExerciseSpec(
                id = "gen_${safe}_chunk_${index + 1}",
                kcId = lexeme.id,
                domain = KcDomain.CHUNK,
                kind = "TEACH",
                promptEs = "Escucha una combinación frecuente con ${lexeme.lemma}.",
                stimulusEn = chunk,
                explanationEs = "Aprende la combinación como una unidad de uso."
            )
        }
        return out
    }

    private fun stableCorrectA(key: String): Boolean = (key.hashCode() and 1) == 0

    private fun plausibleMisspelling(word: String): String {
        if (word.length <= 2) return word + word.last()
        val lower = word.lowercase(Locale.ROOT)
        return when {
            "ough" in lower -> lower.replaceFirst("ough", "ogh")
            "ie" in lower -> lower.replaceFirst("ie", "ei")
            "ee" in lower -> lower.replaceFirst("ee", "e")
            lower.endsWith("e") -> lower.dropLast(1)
            else -> lower.removeRange(lower.length / 2, lower.length / 2 + 1)
        }.ifBlank { lower + "e" }
    }

    private fun idSafe(value: String): String = value
        .lowercase(Locale.ROOT)
        .map { if (it.isLetterOrDigit()) it else '_' }
        .joinToString("")
        .replace(Regex("_+"), "_")
        .trim('_')
}
