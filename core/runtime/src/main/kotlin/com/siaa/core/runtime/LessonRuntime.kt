package com.siaa.core.runtime

import com.siaa.core.algorithm.ExercisePlanner
import com.siaa.core.algorithm.QMatrixDiagnostic
import com.siaa.core.algorithm.OnlineAdaptiveModels
import com.siaa.core.algorithm.StateUpdater
import com.siaa.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LessonRuntime(
    private val repository: LearningRepository,
    private val planner: ExercisePlanner,
    private val stateUpdater: StateUpdater,
    private val speech: SpeechPort,
    private val earcon: EarconPort,
    private val diagnostic: QMatrixDiagnostic = QMatrixDiagnostic(),
    private val onlineModels: OnlineAdaptiveModels = OnlineAdaptiveModels(),
    private val clock: ClockPort = SystemClockPort,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _snapshot = MutableStateFlow(RuntimeSnapshot())
    val snapshot: StateFlow<RuntimeSnapshot> = _snapshot.asStateFlow()

    private var config = SessionConfig()
    private var activeExercise: ExerciseDefinition? = null
    private var activePlannerScore: Double? = null
    private var currentTurn = TurnContext()
    private val answerAccepted = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastPrompt: suspend () -> Unit = {}
    private var sessionJob: Job? = null
    private var timeoutJob: Job? = null

    private suspend fun speakText(text: String, languageTag: String = "es-PE", rate: Float = 1.0f) {
        speech.speak(text, languageTag, (config.speechRate * rate).coerceIn(0.5f, 2.0f))
    }

    fun start(config: SessionConfig = SessionConfig()) {
        if (_snapshot.value.state !in setOf(LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR)) return
        this.config = config
        sessionJob?.cancel()
        timeoutJob?.cancel()
        sessionJob = scope.launch {
            try {
                val id = repository.createSession(config.mode, clock.nowEpochMs())
                currentTurn = TurnContext(turnId = 0L)
                answerAccepted.set(false)
                recordEvent(
                    eventType = RuntimeEventType.SESSION_STARTED,
                    stateBefore = _snapshot.value.state.name,
                    stateAfter = LessonState.PREPARING.name,
                    payload = "Mode: ${config.mode.name}, maxItems: ${config.maxItems}"
                )
                transitionTo(
                    newState = LessonState.PREPARING,
                    message = "Preparando sesión",
                    updateSnapshot = { it.copy(mode = config.mode, sessionId = id, turnId = 0L) }
                )
                speakText(introText(config.mode), "es-PE")
                nextExercise()
            } catch (t: Throwable) {
                fail(t)
            }
        }
    }

    fun stop() {
        timeoutJob?.cancel()
        speech.stop()
        scope.launch { finishSession("Sesión finalizada") }
    }

    fun shutdown() {
        timeoutJob?.cancel()
        speech.stop()
        speech.shutdown()
        earcon.release()
        scope.cancel()
    }

    fun pauseForRouteChange() {
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.ROUTE_LOST,
                stateBefore = _snapshot.value.state.name,
                stateAfter = LessonState.PAUSED.name,
                payload = "Audio route lost"
            )
        }
        pauseInternal(PauseReason.AUDIO_ROUTE_LOST, "Pausado: se desconectó la salida de audio")
    }

    fun pauseForFocusLoss() {
        pauseInternal(PauseReason.AUDIO_FOCUS_LOST, "Pausado: se perdió el foco de audio")
    }

    private fun pauseInternal(reason: PauseReason, message: String) {
        timeoutJob?.cancel()
        speech.stop()
        val current = _snapshot.value
        if (current.state == LessonState.PAUSED) return
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.PAUSED,
                stateBefore = current.state.name,
                stateAfter = LessonState.PAUSED.name,
                payload = reason.name
            )
            transitionTo(
                newState = LessonState.PAUSED,
                message = message,
                updateSnapshot = {
                    it.copy(
                        pausedFrom = current.state,
                        pauseReason = reason
                    )
                }
            )
        }
    }


    fun onCommand(command: RuntimeCommand): Boolean {
        val snap = _snapshot.value
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.COMMAND_RECEIVED,
                stateBefore = snap.state.name,
                stateAfter = snap.state.name,
                runtimeCommand = command.name
            )
        }
        return when (snap.state) {
            LessonState.WAITING_BINARY -> handleBinary(command)
            LessonState.WAITING_SELF_ASSESSMENT -> handleSelfAssessment(command)
            LessonState.SPEAKING, LessonState.FEEDBACK, LessonState.PREPARING, LessonState.HELPING -> handleDuringSpeech(command)
            LessonState.PAUSED -> handlePaused(command)
            else -> if (command == RuntimeCommand.STOP) { stop(); true } else false
        }
    }

    private fun handleBinary(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> {
            if (!answerAccepted.compareAndSet(false, true)) {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = "A"
                    )
                }
                true
            } else {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_ACCEPTED,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = "A"
                    )
                }
                timeoutJob?.cancel()
                answerBinary("A")
                true
            }
        }
        RuntimeCommand.SECONDARY -> {
            if (!answerAccepted.compareAndSet(false, true)) {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = "B"
                    )
                }
                true
            } else {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_ACCEPTED,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = "B"
                    )
                }
                timeoutJob?.cancel()
                answerBinary("B")
                true
            }
        }
        RuntimeCommand.BACK -> { timeoutJob?.cancel(); requestHelp(); true }
        RuntimeCommand.PAUSE -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handleSelfAssessment(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> {
            if (!answerAccepted.compareAndSet(false, true)) {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.CORRECT.name
                    )
                }
                true
            } else {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_ACCEPTED,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.CORRECT.name
                    )
                }
                timeoutJob?.cancel()
                answerSelf(ResponseConfidence.CORRECT)
                true
            }
        }
        RuntimeCommand.SECONDARY -> {
            if (!answerAccepted.compareAndSet(false, true)) {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.UNSURE.name
                    )
                }
                true
            } else {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_ACCEPTED,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.UNSURE.name
                    )
                }
                timeoutJob?.cancel()
                answerSelf(ResponseConfidence.UNSURE)
                true
            }
        }
        RuntimeCommand.BACK -> {
            if (!answerAccepted.compareAndSet(false, true)) {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_REJECTED_DUPLICATE,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.WRONG.name
                    )
                }
                true
            } else {
                scope.launch {
                    recordEvent(
                        eventType = RuntimeEventType.ANSWER_ACCEPTED,
                        stateBefore = _snapshot.value.state.name,
                        stateAfter = _snapshot.value.state.name,
                        runtimeCommand = command.name,
                        payload = ResponseConfidence.WRONG.name
                    )
                }
                timeoutJob?.cancel()
                answerSelf(ResponseConfidence.WRONG)
                true
            }
        }
        RuntimeCommand.PAUSE -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handleDuringSpeech(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PAUSE, RuntimeCommand.PRIMARY -> { pauseManually(); true }
        RuntimeCommand.BACK -> { requestHelp(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handlePaused(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY, RuntimeCommand.PLAY -> {
            scope.launch { resumeFromPause() }
            true
        }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun pauseManually() {
        pauseInternal(PauseReason.USER_REQUESTED, "Pausado")
    }

    private suspend fun resumeFromPause() {
        val pausedFrom = _snapshot.value.pausedFrom ?: LessonState.PREPARING
        recordEvent(
            eventType = RuntimeEventType.RESUMED,
            stateBefore = LessonState.PAUSED.name,
            stateAfter = pausedFrom.name
        )
        transitionTo(
            newState = pausedFrom,
            message = "Reanudando",
            updateSnapshot = { it.copy(pausedFrom = null, pauseReason = null) }
        )
        lastPrompt()
    }

    private fun requestHelp() {
        val exercise = activeExercise ?: return
        scope.launch {
            timeoutJob?.cancel()
            val previousState = _snapshot.value.state
            val depth = (_snapshot.value.helpDepth + 1).coerceAtMost(4)
            currentTurn = currentTurn.copy(helpDepth = depth)
            recordEvent(
                eventType = RuntimeEventType.HELP_REQUESTED,
                stateBefore = previousState.name,
                stateAfter = LessonState.HELPING.name,
                payload = "Depth: $depth"
            )
            transitionTo(
                newState = LessonState.HELPING,
                message = "Ayuda nivel $depth",
                updateSnapshot = { it.copy(helpDepth = depth) }
            )
            when (depth) {
                1 -> {
                    speakText("Repetimos.", "es-PE")
                    if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
                    if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
                }
                2 -> {
                    speakText("Escucha más despacio.", "es-PE")
                    if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US", rate = 0.72f)
                    else speakText(exercise.promptEs, "es-PE", rate = 0.82f)
                }
                3 -> {
                    speakText("Descomposición por palabras.", "es-PE")
                    val words = exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (words.isEmpty()) {
                        speakText(exercise.promptEs, "es-PE", rate = 0.82f)
                    } else {
                        words.forEach { word -> speakText(word, "en-US", rate = 0.78f) }
                    }
                }
                else -> {
                    if (exercise.spellTarget.isNotBlank()) {
                        speakText("Deletreo de la palabra objetivo.", "es-PE")
                        speakText(com.siaa.core.model.EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US", rate = 0.80f)
                    } else {
                        speakText("No hay una palabra objetivo marcada para deletreo. Repetimos la frase por palabras.", "es-PE")
                        exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                            .forEach { word -> speakText(word, "en-US", rate = 0.75f) }
                    }
                }
            }
            if (previousState == LessonState.WAITING_BINARY && exercise.optionA.isNotBlank() && exercise.optionB.isNotBlank()) {
                speakText("Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.", optionLanguage(exercise), rate = if (depth >= 2) 0.82f else 1.0f)
            }
            val finishedAt = clock.nowEpochMs()
            currentTurn = currentTurn.copy(promptFinishedAtEpochMs = finishedAt)
            answerAccepted.set(false)
            transitionTo(
                newState = previousState,
                message = if (previousState == LessonState.WAITING_BINARY) "Esperando A o B" else "Autoevaluación",
                updateSnapshot = { it.copy(promptFinishedAtMs = finishedAt) }
            )
            scheduleTimeout(previousState)
        }
    }

    private fun answerBinary(response: String) {
        val exercise = activeExercise ?: return
        val correct = response.equals(exercise.correctOption, ignoreCase = true)
        scope.launch { processAnswer(exercise, response, correct, null, graded = true, kind = InteractionKind.GRADED_RESPONSE) }
    }

    private fun answerSelf(confidence: ResponseConfidence) {
        val exercise = activeExercise ?: return
        val correct = confidence == ResponseConfidence.CORRECT
        scope.launch { processAnswer(exercise, confidence.name, correct, confidence, graded = true, kind = InteractionKind.GRADED_RESPONSE) }
    }

    private fun scheduleTimeout(waitingState: LessonState) {
        timeoutJob?.cancel()
        val timeoutMs = if (waitingState == LessonState.WAITING_BINARY) {
            config.policy.binaryResponseTimeoutMs
        } else {
            config.policy.selfAssessmentTimeoutMs
        }
        val targetTurnId = currentTurn.turnId
        timeoutJob = scope.launch {
            delay(timeoutMs)
            if (currentTurn.turnId == targetTurnId && _snapshot.value.state == waitingState) {
                handleTimeout(waitingState)
            }
        }
    }

    private suspend fun handleTimeout(waitingState: LessonState) {
        val exercise = activeExercise ?: return
        val retries = currentTurn.timeoutRetries + 1
        if (retries <= config.policy.maxTimeoutRetries) {
            currentTurn = currentTurn.copy(timeoutRetries = retries)
            recordEvent(
                eventType = RuntimeEventType.TIMEOUT,
                stateBefore = waitingState.name,
                stateAfter = _snapshot.value.state.name,
                payload = "Retry $retries"
            )
            earcon.play(EarconKind.ATTENTION)
            delay(140L)
            speakText("¿Sigues ahí? Repetimos la actividad.", "es-PE")
            lastPrompt()
        } else {
            if (answerAccepted.compareAndSet(false, true)) {
                recordEvent(
                    eventType = RuntimeEventType.TIMEOUT,
                    stateBefore = waitingState.name,
                    stateAfter = LessonState.EVALUATING.name,
                    payload = "Max retries exceeded"
                )
                processAnswer(
                    exercise = exercise,
                    response = "TIMEOUT",
                    correct = false,
                    confidence = null,
                    graded = false,
                    kind = InteractionKind.TIMEOUT
                )
            }
        }
    }

    private suspend fun processAnswer(
        exercise: ExerciseDefinition,
        response: String,
        correct: Boolean,
        confidence: ResponseConfidence?,
        graded: Boolean,
        kind: InteractionKind
    ) {
        timeoutJob?.cancel()
        val snap = _snapshot.value
        val now = clock.nowEpochMs()
        val latency = snap.promptFinishedAtMs?.let { (now - it).coerceAtLeast(0L) }
        val beforeSnapshot = repository.loadSnapshot()
        val primaryKcId = exercise.kcIds.firstOrNull()
        val beforeMastery = primaryKcId?.let { beforeSnapshot.stateByKcId[it]?.mastery }

        transitionTo(newState = LessonState.EVALUATING, message = "Evaluando respuesta")

        val diagnosticWeights = diagnostic.diagnosticWeights(exercise, beforeSnapshot.stateByKcId)
        val updatedStates = exercise.kcIds.mapNotNull { kcId ->
            val component = beforeSnapshot.componentById[kcId] ?: return@mapNotNull null
            val prior = beforeSnapshot.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)

            val qWeight = diagnosticWeights[kcId] ?: (1.0 / exercise.kcIds.size.coerceAtLeast(1))
            val evidenceWeight = if (!graded) {
                0.15 * qWeight
            } else if (correct) {
                (0.55 + 0.45 * qWeight).coerceAtMost(1.0)
            } else {
                (0.25 + 0.75 * qWeight).coerceAtMost(1.0)
            }

            val elapsedHours = prior.lastReviewedAtEpochMs?.let { last ->
                (now - last).coerceAtLeast(0L) / 3_600_000.0
            } ?: 0.0
            if (prior.lastReviewedAtEpochMs != null && graded) {
                onlineModels.observeMemory(kcId, elapsedHours, prior.halfLifeHours, correct)
            }
            val baseUpdate = if (graded) {
                stateUpdater.update(component, prior, exercise, correct, confidence, latency, now, evidenceWeight)
            } else {
                prior.copy(
                    uncertainty = (prior.uncertainty + 0.05).coerceAtMost(0.95),
                    lastReviewedAtEpochMs = now,
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            }
            val particle = if (graded) {
                onlineModels.observeLatentState(
                    kcId = kcId,
                    priorMastery = prior.mastery,
                    priorHalfLifeHours = prior.halfLifeHours,
                    correct = correct,
                    elapsedHours = elapsedHours
                )
            } else null

            if (particle != null) {
                baseUpdate.copy(
                    mastery = (0.78 * baseUpdate.mastery + 0.22 * particle.masteryMean).coerceIn(0.0, 1.0),
                    halfLifeHours = (0.88 * baseUpdate.halfLifeHours + 0.12 * particle.halfLifeMean).coerceIn(0.25, 24.0 * 365.0),
                    uncertainty = (0.75 * baseUpdate.uncertainty + 0.25 * (particle.masterySd * 2.5).coerceIn(0.04, 0.95)).coerceIn(0.04, 0.95),
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            } else {
                baseUpdate.copy(
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            }
        }

        val afterMastery = primaryKcId?.let { id -> updatedStates.firstOrNull { it.kcId == id }?.mastery }
        if (primaryKcId != null && graded) {
            val masteryGain = ((afterMastery ?: beforeMastery ?: 0.0) - (beforeMastery ?: 0.0)).coerceIn(-0.20, 0.20)
            val accuracyReward = if (correct) 0.70 else 0.0
            val speedReward = latency?.let { ms ->
                val target = (exercise.estimatedSeconds.coerceAtLeast(3) * 1000.0 * 0.30).coerceAtLeast(1200.0)
                (1.0 - (ms / (target * 2.0))).coerceIn(0.0, 1.0) * 0.15
            } ?: 0.05
            val learningReward = ((masteryGain + 0.20) / 0.40).coerceIn(0.0, 1.0) * 0.15
            onlineModels.observeStrategy(primaryKcId, exercise.type, (accuracyReward + speedReward + learningReward).coerceIn(0.0, 1.0))
        }

        val sessionId = snap.sessionId ?: return
        val interactionRecord = InteractionRecord(
            sessionId = sessionId,
            turnId = currentTurn.turnId,
            exerciseId = exercise.id,
            timestampEpochMs = now,
            response = response,
            correct = correct,
            graded = graded,
            kind = kind,
            confidence = confidence,
            latencyMs = latency,
            hintDepth = snap.helpDepth,
            plannerScore = activePlannerScore,
            stateBeforeMastery = beforeMastery,
            stateAfterMastery = afterMastery
        )

        repository.commitTurn(
            interaction = interactionRecord,
            updatedStates = updatedStates,
            misconceptionUpdates = emptyList()
        )
        recordEvent(
            eventType = RuntimeEventType.TURN_COMMITTED,
            stateBefore = LessonState.EVALUATING.name,
            stateAfter = LessonState.EVALUATING.name,
            exerciseId = exercise.id,
            payload = "Turn ${currentTurn.turnId} committed"
        )

        val newCompleted = snap.completedItems + (if (graded) 1 else 0)
        val newCorrect = snap.correctItems + (if (graded && correct) 1 else 0)
        transitionTo(
            newState = LessonState.FEEDBACK,
            message = if (!graded) "Sin respuesta" else if (correct) "Correcto" else "Necesita ajuste",
            updateSnapshot = {
                it.copy(
                    completedItems = newCompleted,
                    correctItems = newCorrect
                )
            }
        )

        val feedback = if (!graded) {
            "Tiempo agotado. Pasamos a la siguiente actividad."
        } else if (correct) {
            if (exercise.explanationEs.isNotBlank() && config.feedbackExplanations) "Correcto. ${exercise.explanationEs}" else "Correcto."
        } else {
            buildString {
                append("La respuesta correcta es ${exercise.correctOption.ifBlank { "la indicada" }}. ")
                if (exercise.explanationEs.isNotBlank()) append(exercise.explanationEs)
            }
        }
        recordEvent(
            eventType = RuntimeEventType.FEEDBACK,
            stateBefore = LessonState.EVALUATING.name,
            stateAfter = LessonState.FEEDBACK.name,
            exerciseId = exercise.id,
            payload = feedback.trim()
        )
        earcon.play(if (correct) EarconKind.CORRECT else EarconKind.INCORRECT)
        delay(if (correct) 160L else 200L)
        speakText(feedback.trim(), "es-PE")
        if (_snapshot.value.completedItems >= config.maxItems) finishSession("Objetivo de sesión completado") else nextExercise()
    }

    private suspend fun nextExercise() {
        timeoutJob?.cancel()
        val nextTurnId = currentTurn.turnId + 1
        currentTurn = TurnContext(turnId = nextTurnId)

        transitionTo(
            newState = LessonState.PLANNING_NEXT,
            message = "Planificando siguiente actividad",
            updateSnapshot = { it.copy(turnId = nextTurnId) }
        )

        val now = clock.nowEpochMs()
        val learning = repository.loadSnapshot()
        val recent = repository.recentInteractions(80)
        val candidate = planner.choose(config.mode, learning, recent, now, config.capabilities)
        if (candidate == null) {
            finishSession("No quedan actividades elegibles")
            return
        }
        recordEvent(
            eventType = RuntimeEventType.EXERCISE_SELECTED,
            stateBefore = _snapshot.value.state.name,
            stateAfter = LessonState.PLANNING_NEXT.name,
            exerciseId = candidate.exercise.id,
            payload = candidate.rationale
        )
        activeExercise = candidate.exercise
        activePlannerScore = candidate.utility
        currentTurn = currentTurn.copy(exerciseId = candidate.exercise.id)
        answerAccepted.set(false)

        transitionTo(
            newState = LessonState.PREPARING,
            message = "Siguiente actividad",
            updateSnapshot = {
                it.copy(
                    currentExerciseId = candidate.exercise.id,
                    currentKcId = candidate.exercise.kcIds.firstOrNull(),
                    lastPlannerRationale = candidate.rationale,
                    helpDepth = 0
                )
            }
        )
        present(candidate.exercise)
    }

    private suspend fun present(exercise: ExerciseDefinition) {
        when (exercise.type) {
            ExerciseType.TEACH -> presentTeaching(exercise)
            ExerciseType.SELF_ASSESS, ExerciseType.SPELL_FROM_AUDIO -> presentSelfAssessment(exercise)
            else -> presentBinary(exercise)
        }
    }

    private suspend fun presentTeaching(exercise: ExerciseDefinition) {
        val prompt: suspend () -> Unit = {
            timeoutJob?.cancel()
            recordEvent(
                eventType = RuntimeEventType.PROMPT_STARTED,
                stateBefore = _snapshot.value.state.name,
                stateAfter = LessonState.SPEAKING.name,
                exerciseId = exercise.id
            )
            transitionTo(newState = LessonState.SPEAKING, message = "Explicación")
            earcon.play(EarconKind.NEW_PROMPT)
            delay(90L)
            if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank()) {
                speakText("Se escribe", "es-PE")
                speakText(com.siaa.core.model.EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US")
            }
            if (exercise.explanationEs.isNotBlank()) speakText(exercise.explanationEs, "es-PE")
            val snap = _snapshot.value
            val learning = repository.loadSnapshot()
            val now = clock.nowEpochMs()
            val updatedStates = exercise.kcIds.mapNotNull { kcId ->
                val component = learning.componentById[kcId] ?: return@mapNotNull null
                val prior = learning.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
                prior.copy(
                    mastery = (prior.mastery + 0.04 * (1.0 - prior.mastery)).coerceAtMost(0.97),
                    recognition = (prior.recognition + 0.05).coerceAtMost(1.0),
                    halfLifeHours = (prior.halfLifeHours * 1.08).coerceAtMost(24.0 * 365.0),
                    lastReviewedAtEpochMs = now,
                    totalAttempts = prior.totalAttempts + 1,
                    exposureCount = prior.exposureCount + 1,
                    lastExposedAtEpochMs = now
                )
            }
            val sessionId = snap.sessionId
            if (sessionId != null) {
                repository.commitTurn(
                    interaction = InteractionRecord(
                        sessionId = sessionId,
                        turnId = currentTurn.turnId,
                        exerciseId = exercise.id,
                        timestampEpochMs = now,
                        response = "TEACH_PRESENTED",
                        correct = true,
                        graded = false,
                        kind = InteractionKind.TEACH_EXPOSURE,
                        plannerScore = activePlannerScore
                    ),
                    updatedStates = updatedStates,
                    misconceptionUpdates = emptyList()
                )
                recordEvent(
                    eventType = RuntimeEventType.TURN_COMMITTED,
                    stateBefore = LessonState.SPEAKING.name,
                    stateAfter = LessonState.SPEAKING.name,
                    exerciseId = exercise.id,
                    payload = "Turn ${currentTurn.turnId} committed (teach)"
                )
            }
            _snapshot.value = _snapshot.value.copy(completedItems = snap.completedItems + 1)
            nextExercise()
        }
        lastPrompt = prompt
        prompt()
    }

    private suspend fun presentBinary(exercise: ExerciseDefinition) {
        val prompt: suspend () -> Unit = {
            timeoutJob?.cancel()
            recordEvent(
                eventType = RuntimeEventType.PROMPT_STARTED,
                stateBefore = _snapshot.value.state.name,
                stateAfter = LessonState.SPEAKING.name,
                exerciseId = exercise.id
            )
            transitionTo(newState = LessonState.SPEAKING, message = "Pregunta")
            earcon.play(EarconKind.NEW_PROMPT)
            delay(90L)
            if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank() && exercise.type == ExerciseType.SPELLING_AB) {
                speakText("Palabra objetivo: ${exercise.spellTarget}", "es-PE")
            }
            speakText("Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.", optionLanguage(exercise))
            if (config.announceControls) speakText("Play pausa para A. Siguiente para B. Anterior para repetir.", "es-PE")
            val finishedAt = clock.nowEpochMs()
            currentTurn = currentTurn.copy(promptFinishedAtEpochMs = finishedAt, expectedState = LessonState.WAITING_BINARY)
            answerAccepted.set(false)
            recordEvent(
                eventType = RuntimeEventType.PROMPT_FINISHED,
                stateBefore = LessonState.SPEAKING.name,
                stateAfter = LessonState.WAITING_BINARY.name,
                exerciseId = exercise.id
            )
            transitionTo(
                newState = LessonState.WAITING_BINARY,
                message = "Esperando A o B",
                updateSnapshot = { it.copy(promptFinishedAtMs = finishedAt) }
            )
            scheduleTimeout(LessonState.WAITING_BINARY)
        }
        lastPrompt = prompt
        prompt()
    }

    private suspend fun presentSelfAssessment(exercise: ExerciseDefinition) {
        val prompt: suspend () -> Unit = {
            timeoutJob?.cancel()
            recordEvent(
                eventType = RuntimeEventType.PROMPT_STARTED,
                stateBefore = _snapshot.value.state.name,
                stateAfter = LessonState.SPEAKING.name,
                exerciseId = exercise.id
            )
            transitionTo(newState = LessonState.SPEAKING, message = "Recuperación mental")
            earcon.play(EarconKind.NEW_PROMPT)
            delay(90L)
            if (exercise.promptEs.isNotBlank()) speakText(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speakText(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank()) {
                speakText("Forma mentalmente el deletreo de ${exercise.spellTarget}.", "es-PE")
            }
            delay(2200)
            speakText("¿Lo resolviste? Play pausa para sí. Siguiente para dudé. Anterior para no.", "es-PE")
            val finishedAt = clock.nowEpochMs()
            currentTurn = currentTurn.copy(promptFinishedAtEpochMs = finishedAt, expectedState = LessonState.WAITING_SELF_ASSESSMENT)
            answerAccepted.set(false)
            recordEvent(
                eventType = RuntimeEventType.PROMPT_FINISHED,
                stateBefore = LessonState.SPEAKING.name,
                stateAfter = LessonState.WAITING_SELF_ASSESSMENT.name,
                exerciseId = exercise.id
            )
            transitionTo(
                newState = LessonState.WAITING_SELF_ASSESSMENT,
                message = "Autoevaluación",
                updateSnapshot = { it.copy(promptFinishedAtMs = finishedAt) }
            )
            scheduleTimeout(LessonState.WAITING_SELF_ASSESSMENT)
        }
        lastPrompt = prompt
        prompt()
    }

    private suspend fun transitionTo(
        newState: LessonState,
        message: String,
        updateSnapshot: (RuntimeSnapshot) -> RuntimeSnapshot = { it }
    ) {
        val old = _snapshot.value
        val updated = updateSnapshot(old.copy(state = newState, message = message))
        _snapshot.value = updated
        recordEvent(
            eventType = RuntimeEventType.STATE_TRANSITION,
            stateBefore = old.state.name,
            stateAfter = newState.name,
            exerciseId = activeExercise?.id
        )
    }

    private suspend fun recordEvent(
        eventType: RuntimeEventType,
        stateBefore: String,
        stateAfter: String,
        exerciseId: String? = activeExercise?.id,
        runtimeCommand: String? = null,
        mediaKeyCode: Int? = null,
        payload: String? = null
    ) {
        val sessionId = _snapshot.value.sessionId ?: return
        repository.recordRuntimeEvent(
            RuntimeEvent(
                sessionId = sessionId,
                turnId = currentTurn.turnId,
                timestampEpochMs = clock.nowEpochMs(),
                eventType = eventType,
                stateBefore = stateBefore,
                stateAfter = stateAfter,
                exerciseId = exerciseId,
                runtimeCommand = runtimeCommand,
                mediaKeyCode = mediaKeyCode,
                payload = payload
            )
        )
    }

    private fun optionLanguage(exercise: ExerciseDefinition): String = when (exercise.type) {
        ExerciseType.MEANING_AB -> "es-PE"
        else -> "en-US"
    }

    private suspend fun finishSession(message: String) {
        timeoutJob?.cancel()
        speech.stop()
        val snap = _snapshot.value
        recordEvent(
            eventType = RuntimeEventType.SESSION_STOPPED,
            stateBefore = snap.state.name,
            stateAfter = LessonState.SESSION_END.name,
            payload = message
        )
        snap.sessionId?.let { repository.finishSession(it, clock.nowEpochMs()) }
        transitionTo(
            newState = LessonState.SESSION_END,
            message = message,
            updateSnapshot = { it.copy(promptFinishedAtMs = null) }
        )
        earcon.play(EarconKind.REGISTERED)
        delay(110L)
        speakText(message, "es-PE")
    }

    private fun fail(t: Throwable) {
        timeoutJob?.cancel()
        speech.stop()
        scope.launch {
            recordEvent(
                eventType = RuntimeEventType.ERROR,
                stateBefore = _snapshot.value.state.name,
                stateAfter = LessonState.ERROR.name,
                payload = t.message ?: t::class.java.simpleName
            )
        }
        _snapshot.value = _snapshot.value.copy(state = LessonState.ERROR, message = "Error", error = t.message ?: t::class.java.simpleName)
        earcon.play(EarconKind.WARNING)
    }

    private fun introText(mode: SessionMode): String = when (mode) {
        SessionMode.ADAPTIVE -> "Sesión adaptativa iniciada. Guarda el teléfono."
        SessionMode.VOCABULARY -> "Modo vocabulario iniciado. Trabajaremos significado, sonido, escritura y uso."
        SessionMode.GRAMMAR -> "Modo gramática iniciado."
        SessionMode.LISTENING -> "Modo comprensión auditiva iniciado."
        SessionMode.SPELLING -> "Modo ortografía y deletreo iniciado."
        SessionMode.PRONUNCIATION -> "Modo discriminación de pronunciación iniciado."
    }
}

