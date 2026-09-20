package com.siaa.core.data

import android.content.Context
import com.siaa.core.content.ContentPackValidator
import com.siaa.core.content.ControlledVariantGenerator
import com.siaa.core.content.LexemeSpec
import com.siaa.core.content.ContentValidationIssue
import org.json.JSONArray
import androidx.room.withTransaction

class ContentSeeder(
    private val context: Context,
    private val db: SiaaDatabase
) {
    companion object { const val CONTENT_VERSION = "0.7.0" }
    suspend fun seedIfNeeded() {
        val installedVersion = db.metaDao().get("content_version")
        if (installedVersion == CONTENT_VERSION && db.contentDao().kcCount() > 0) return
        val kcs = readArray("content/kcs.json").map { obj ->
            KnowledgeComponentEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                cefr = obj.optString("cefr", "A1"),
                domain = obj.getString("domain"),
                form = obj.optString("form"),
                meaning = obj.optString("meaning"),
                useText = obj.optString("use"),
                importance = obj.optDouble("importance", 0.5),
                priorMastery = obj.optDouble("priorMastery", 0.15),
                tagsCsv = jsonArrayToCsv(obj.optJSONArray("tags"))
            )
        }
        val edges = readArray("content/edges.json").map { obj ->
            KnowledgeEdgeEntity(
                fromId = obj.getString("fromId"),
                toId = obj.getString("toId"),
                weight = obj.optDouble("weight", 1.0),
                hardPrerequisite = obj.optBoolean("hardPrerequisite", true)
            )
        }
        val staticExercises = readArray("content/exercises.json").map { obj ->
            ExerciseEntity(
                id = obj.getString("id"),
                type = obj.getString("type"),
                kcIdsCsv = jsonArrayToCsv(obj.getJSONArray("kcIds")),
                cefr = obj.optString("cefr", "A1"),
                difficulty = obj.optDouble("difficulty", 0.4),
                promptEs = obj.optString("promptEs"),
                stimulusEn = obj.optString("stimulusEn"),
                optionA = obj.optString("optionA"),
                optionB = obj.optString("optionB"),
                correctOption = obj.optString("correctOption"),
                explanationEs = obj.optString("explanationEs"),
                spellTarget = obj.optString("spellTarget"),
                estimatedSeconds = obj.optInt("estimatedSeconds", 20),
                tagsCsv = jsonArrayToCsv(obj.optJSONArray("tags"))
            )
        }
        val generator = ControlledVariantGenerator()
        val generatedExercises = readArray("content/lexemes.json")
            .map { obj ->
                LexemeSpec(
                    id = obj.getString("id"),
                    lemma = obj.getString("lemma"),
                    meaningEs = obj.getString("meaningEs"),
                    cefr = obj.optString("cefr", "A1"),
                    spellingDifficulty = obj.optDouble("spellingDifficulty", 0.25),
                    distractorEs = obj.optString("distractorEs", "otra opción"),
                    chunks = jsonArrayToList(obj.optJSONArray("chunks")),
                    tags = jsonArrayToList(obj.optJSONArray("tags")).toSet()
                )
            }
            .flatMap(generator::vocabularyVariants)
            .map { gen ->
                ExerciseEntity(
                    id = gen.id,
                    type = gen.kind,
                    kcIdsCsv = gen.kcId,
                    cefr = kcs.firstOrNull { it.id == gen.kcId }?.cefr ?: "A1",
                    difficulty = if (gen.kind == "SPELLING_AB" || gen.kind == "SPELL_FROM_AUDIO") 0.48 else 0.35,
                    promptEs = gen.promptEs,
                    stimulusEn = gen.stimulusEn,
                    optionA = gen.optionA,
                    optionB = gen.optionB,
                    correctOption = gen.correctOption,
                    explanationEs = gen.explanationEs,
                    spellTarget = gen.spellTarget,
                    estimatedSeconds = if (gen.kind == "TEACH") 18 else 22,
                    tagsCsv = "generated,vocabulary"
                )
            }
        val exercises = (staticExercises + generatedExercises).distinctBy { it.id }

        val validation = ContentPackValidator.validate(
            components = kcs.map { it.toModel() },
            edges = edges.map { it.toModel() },
            exercises = exercises.map { it.toModel() }
        )
        val errors = validation.filter { it.severity == ContentValidationIssue.Severity.ERROR }
        check(errors.isEmpty()) {
            "El content pack local es inválido: " + errors.take(8).joinToString(" | ") { "${it.code}: ${it.message}" }
        }

        db.withTransaction {
            // El content pack es versionado: reemplazamos definiciones, preservando el modelo del alumno.
            db.contentDao().clearEdges()
            db.contentDao().clearExercises()
            db.contentDao().clearKcs()
            db.contentDao().insertKcs(kcs)
            db.contentDao().insertEdges(edges)
            db.contentDao().insertExercises(exercises)
            db.metaDao().put(AppMetaEntity("content_version", CONTENT_VERSION))
        }
    }

    private fun readArray(path: String): List<org.json.JSONObject> {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        return List(arr.length()) { arr.getJSONObject(it) }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    private fun jsonArrayToCsv(arr: JSONArray?): String {
        if (arr == null) return ""
        return (0 until arr.length()).joinToString(",") { arr.optString(it) }
    }
}
