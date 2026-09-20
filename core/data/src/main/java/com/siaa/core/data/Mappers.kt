package com.siaa.core.data

import com.siaa.core.model.*

internal fun KnowledgeComponentEntity.toModel() = KnowledgeComponent(
    id, name, cefr, KcDomain.valueOf(domain), form, meaning, useText, importance, priorMastery, csvToSet(tagsCsv)
)
internal fun KnowledgeEdgeEntity.toModel() = KnowledgeEdge(fromId, toId, weight, hardPrerequisite)
internal fun LearnerKcStateEntity.toModel() = LearnerKcState(
    kcId, mastery, recognition, production, orthography, automaticity, halfLifeHours, uncertainty,
    lastReviewedAtEpochMs, consecutiveSuccess, consecutiveFailure, totalAttempts, totalCorrect
)
internal fun LearnerKcState.toEntity() = LearnerKcStateEntity(
    kcId, mastery, recognition, production, orthography, automaticity, halfLifeHours, uncertainty,
    lastReviewedAtEpochMs, consecutiveSuccess, consecutiveFailure, totalAttempts, totalCorrect
)
internal fun ExerciseEntity.toModel() = ExerciseDefinition(
    id = id,
    type = ExerciseType.valueOf(type),
    kcIds = csvToList(kcIdsCsv),
    cefr = cefr,
    difficulty = difficulty,
    promptEs = promptEs,
    stimulusEn = stimulusEn,
    optionA = optionA,
    optionB = optionB,
    correctOption = correctOption,
    explanationEs = explanationEs,
    spellTarget = spellTarget,
    estimatedSeconds = estimatedSeconds,
    tags = csvToSet(tagsCsv)
)
internal fun InteractionEntity.toModel() = InteractionRecord(
    id, sessionId, exerciseId, timestampEpochMs, response, correct,
    confidence?.let(ResponseConfidence::valueOf), latencyMs, hintDepth, plannerScore, stateBeforeMastery, stateAfterMastery
)
internal fun InteractionRecord.toEntity() = InteractionEntity(
    id, sessionId, exerciseId, timestampEpochMs, response, correct,
    confidence?.name, latencyMs, hintDepth, plannerScore, stateBeforeMastery, stateAfterMastery
)
internal fun DeviceProfileEntity.toModel() = DeviceProfile(id, name, playPauseAvailable, nextAvailable, previousAvailable, lastSeenAtEpochMs)
internal fun DeviceProfile.toEntity() = DeviceProfileEntity(id, name, playPauseAvailable, nextAvailable, previousAvailable, lastSeenAtEpochMs)
internal fun MisconceptionEntity.toModel() = Misconception(id, kcId, label, probability, lastObservedAtEpochMs)

internal fun csvToList(csv: String): List<String> = csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
internal fun csvToSet(csv: String): Set<String> = csvToList(csv).toSet()
internal fun Iterable<String>.toCsv(): String = joinToString(",")
