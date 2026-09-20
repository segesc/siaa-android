package com.siaa.core.data

import com.siaa.core.model.*

internal fun KnowledgeComponentEntity.toModel() = KnowledgeComponent(
    id, name, cefr, KcDomain.valueOf(domain), form, meaning, useText, importance, priorMastery, csvToSet(tagsCsv)
)
internal fun KnowledgeEdgeEntity.toModel() = KnowledgeEdge(fromId, toId, weight, hardPrerequisite)
internal fun LearnerKcStateEntity.toModel() = LearnerKcState(
    kcId, mastery, recognition, production, orthography, automaticity, halfLifeHours, uncertainty,
    lastReviewedAtEpochMs, consecutiveSuccess, consecutiveFailure, totalAttempts, totalCorrect,
    exposureCount, lastExposedAtEpochMs
)
internal fun LearnerKcState.toEntity() = LearnerKcStateEntity(
    kcId, mastery, recognition, production, orthography, automaticity, halfLifeHours, uncertainty,
    lastReviewedAtEpochMs, consecutiveSuccess, consecutiveFailure, totalAttempts, totalCorrect,
    exposureCount, lastExposedAtEpochMs
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
    tags = csvToSet(tagsCsv),
    misconceptionIds = csvToList(misconceptionIdsCsv)
)
internal fun InteractionEntity.toModel() = InteractionRecord(
    id = id,
    sessionId = sessionId,
    turnId = turnId,
    exerciseId = exerciseId,
    timestampEpochMs = timestampEpochMs,
    response = response,
    correct = correct,
    graded = graded,
    kind = runCatching { InteractionKind.valueOf(kind) }.getOrDefault(InteractionKind.GRADED_RESPONSE),
    confidence = confidence?.let(ResponseConfidence::valueOf),
    latencyMs = latencyMs,
    hintDepth = hintDepth,
    plannerScore = plannerScore,
    stateBeforeMastery = stateBeforeMastery,
    stateAfterMastery = stateAfterMastery
)
internal fun InteractionRecord.toEntity() = InteractionEntity(
    id = id,
    sessionId = sessionId,
    turnId = turnId,
    exerciseId = exerciseId,
    timestampEpochMs = timestampEpochMs,
    response = response,
    correct = correct,
    graded = graded,
    kind = kind.name,
    confidence = confidence?.name,
    latencyMs = latencyMs,
    hintDepth = hintDepth,
    plannerScore = plannerScore,
    stateBeforeMastery = stateBeforeMastery,
    stateAfterMastery = stateAfterMastery
)
internal fun DeviceProfileEntity.toModel() = DeviceProfile(
    id = id,
    name = name,
    primaryKeyCode = primaryKeyCode,
    secondaryKeyCode = secondaryKeyCode,
    backKeyCode = backKeyCode,
    stopKeyCode = stopKeyCode,
    playPauseAvailable = playPauseAvailable,
    nextAvailable = nextAvailable,
    previousAvailable = previousAvailable,
    lastSeenAtEpochMs = lastSeenAtEpochMs
)
internal fun DeviceProfile.toEntity() = DeviceProfileEntity(
    id = id,
    name = name,
    primaryKeyCode = primaryKeyCode,
    secondaryKeyCode = secondaryKeyCode,
    backKeyCode = backKeyCode,
    stopKeyCode = stopKeyCode,
    playPauseAvailable = playPauseAvailable,
    nextAvailable = nextAvailable,
    previousAvailable = previousAvailable,
    lastSeenAtEpochMs = lastSeenAtEpochMs
)
internal fun RuntimeEventEntity.toModel() = RuntimeEvent(
    id = id,
    sessionId = sessionId,
    turnId = turnId,
    timestampEpochMs = timestampEpochMs,
    eventType = RuntimeEventType.valueOf(eventType),
    stateBefore = stateBefore,
    stateAfter = stateAfter,
    exerciseId = exerciseId,
    runtimeCommand = runtimeCommand,
    mediaKeyCode = mediaKeyCode,
    payload = payload
)
internal fun RuntimeEvent.toEntity() = RuntimeEventEntity(
    id = id,
    sessionId = sessionId,
    turnId = turnId,
    timestampEpochMs = timestampEpochMs,
    eventType = eventType.name,
    stateBefore = stateBefore,
    stateAfter = stateAfter,
    exerciseId = exerciseId,
    runtimeCommand = runtimeCommand,
    mediaKeyCode = mediaKeyCode,
    payload = payload
)
internal fun MisconceptionEntity.toModel() = Misconception(id, kcId, label, probability, lastObservedAtEpochMs)
internal fun Misconception.toEntity() = MisconceptionEntity(id, kcId, label, probability, lastObservedAtEpochMs)


internal fun csvToList(csv: String): List<String> = csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
internal fun csvToSet(csv: String): Set<String> = csvToList(csv).toSet()
internal fun Iterable<String>.toCsv(): String = joinToString(",")
