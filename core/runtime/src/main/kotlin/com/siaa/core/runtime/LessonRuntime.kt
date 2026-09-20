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
    private var lastPrompt: suspend () -> Unit = {}
    private var sessionJob: Job? = null

    fun start(config: SessionConfig = SessionConfig()) {
        if (_snapshot.value.state !in setOf(LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR)) return
        this.config = config
        sessionJob?.cancel()
        sessionJob = scope.launch {
            try {
                val id = repository.createSession(config.mode, clock.nowEpochMs())
                _snapshot.value = RuntimeSnapshot(
                    state = LessonState.PREPARING,
                    mode = config.mode,
                    sessionId = id,
                    message = "Preparando sesión"
                )
                speech.speak(introText(config.mode), "es-PE")
                nextExercise()
            } catch (t: Throwable) {
                fail(t)
            }
        }
    }

    fun stop() {
        scope.launch { finishSession("Sesión finalizada") }
    }

    fun shutdown() {
        speech.stop()
        speech.shutdown()
        earcon.release()
        scope.cancel()
    }

    fun pauseForRouteChange() {
        speech.stop()
        _snapshot.value = _snapshot.value.copy(
            state = LessonState.PAUSED,
            message = "Pausado: se desconectó la salida de audio"
        )
    }

    fun onCommand(command: RuntimeCommand): Boolean {
        val snap = _snapshot.value
        return when (snap.state) {
            LessonState.WAITING_BINARY -> handleBinary(command)
            LessonState.WAITING_SELF_ASSESSMENT -> handleSelfAssessment(command)
            LessonState.SPEAKING, LessonState.FEEDBACK, LessonState.PREPARING -> handleDuringSpeech(command)
            LessonState.PAUSED -> handlePaused(command)
            else -> if (command == RuntimeCommand.STOP) { stop(); true } else false
        }
    }

    private fun handleBinary(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> { answerBinary("A"); true }
        RuntimeCommand.SECONDARY -> { answerBinary("B"); true }
        RuntimeCommand.BACK -> { requestHelp(); true }
        RuntimeCommand.PAUSE -> { pauseManually(); true }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun handleSelfAssessment(command: RuntimeCommand): Boolean = when (command) {
        RuntimeCommand.PRIMARY -> { answerSelf(ResponseConfidence.CORRECT); true }
        RuntimeCommand.SECONDARY -> { answerSelf(ResponseConfidence.UNSURE); true }
        RuntimeCommand.BACK -> { answerSelf(ResponseConfidence.WRONG); true }
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
            scope.launch { lastPrompt() }
            true
        }
        RuntimeCommand.STOP -> { stop(); true }
        else -> false
    }

    private fun pauseManually() {
        speech.stop()
        _snapshot.value = _snapshot.value.copy(state = LessonState.PAUSED, message = "Pausado")
    }

    private fun repeatPrompt() {
        scope.launch {
            _snapshot.value = _snapshot.value.copy(helpDepth = _snapshot.value.helpDepth + 1)
            lastPrompt()
        }
    }

    private fun requestHelp() {
        val exercise = activeExercise ?: return
        scope.launch {
            val previousState = _snapshot.value.state
            val depth = (_snapshot.value.helpDepth + 1).coerceAtMost(4)
            _snapshot.value = _snapshot.value.copy(
                state = LessonState.SPEAKING,
                helpDepth = depth,
                message = "Ayuda nivel $depth"
            )
            when (depth) {
                1 -> {
                    speech.speak("Repetimos.", "es-PE")
                    if (exercise.promptEs.isNotBlank()) speech.speak(exercise.promptEs, "es-PE")
                    if (exercise.stimulusEn.isNotBlank()) speech.speak(exercise.stimulusEn, "en-US")
                }
                2 -> {
                    speech.speak("Escucha más despacio.", "es-PE")
                    if (exercise.stimulusEn.isNotBlank()) speech.speak(exercise.stimulusEn, "en-US", rate = 0.72f)
                    else speech.speak(exercise.promptEs, "es-PE", rate = 0.82f)
                }
                3 -> {
                    speech.speak("Descomposición por palabras.", "es-PE")
                    val words = exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (words.isEmpty()) {
                        speech.speak(exercise.promptEs, "es-PE", rate = 0.82f)
                    } else {
                        words.forEach { word -> speech.speak(word, "en-US", rate = 0.78f) }
                    }
                }
                else -> {
                    if (exercise.spellTarget.isNotBlank()) {
                        speech.speak("Deletreo de la palabra objetivo.", "es-PE")
                        speech.speak(com.siaa.core.model.EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US", rate = 0.80f)
                    } else {
                        speech.speak("No hay una palabra objetivo marcada para deletreo. Repetimos la frase por palabras.", "es-PE")
                        exercise.stimulusEn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                            .forEach { word -> speech.speak(word, "en-US", rate = 0.75f) }
                    }
                }
            }
            if (previousState == LessonState.WAITING_BINARY && exercise.optionA.isNotBlank() && exercise.optionB.isNotBlank()) {
                speech.speak("Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.", optionLanguage(exercise), rate = if (depth >= 2) 0.82f else 1.0f)
            }
            _snapshot.value = _snapshot.value.copy(
                state = previousState,
                promptFinishedAtMs = clock.nowEpochMs(),
                message = if (previousState == LessonState.WAITING_BINARY) "Esperando A o B" else "Autoevaluación"
            )
        }
    }

    private fun answerBinary(response: String) {
        val exercise = activeExercise ?: return
        val correct = response.equals(exercise.correctOption, ignoreCase = true)
        scope.launch { processAnswer(exercise, response, correct, null) }
    }

    private fun answerSelf(confidence: ResponseConfidence) {
        val exercise = activeExercise ?: return
        val correct = confidence == ResponseConfidence.CORRECT
        scope.launch { processAnswer(exercise, confidence.name, correct, confidence) }
    }

    private suspend fun processAnswer(
        exercise: ExerciseDefinition,
        response: String,
        correct: Boolean,
        confidence: ResponseConfidence?
    ) {
        val snap = _snapshot.value
        val now = clock.nowEpochMs()
        val latency = snap.promptFinishedAtMs?.let { (now - it).coerceAtLeast(0L) }
        val beforeSnapshot = repository.loadSnapshot()
        val primaryKcId = exercise.kcIds.firstOrNull()
        val beforeMastery = primaryKcId?.let { beforeSnapshot.stateByKcId[it]?.mastery }

        val diagnosticWeights = diagnostic.diagnosticWeights(exercise, beforeSnapshot.stateByKcId)
        val updatedStates = exercise.kcIds.mapNotNull { kcId ->
            val component = beforeSnapshot.componentById[kcId] ?: return@mapNotNull null
            val prior = beforeSnapshot.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
            // En una respuesta correcta, todos los KCs requeridos reciben evidencia moderada.
            // En un fallo, el CDM concentra el crédito diagnóstico en los atributos débiles/inciertos.
            val qWeight = diagnosticWeights[kcId] ?: (1.0 / exercise.kcIds.size.coerceAtLeast(1))
            val evidenceWeight = if (correct) (0.55 + 0.45 * qWeight).coerceAtMost(1.0) else (0.25 + 0.75 * qWeight).coerceAtMost(1.0)
            val elapsedHours = prior.lastReviewedAtEpochMs?.let { last ->
                (now - last).coerceAtLeast(0L) / 3_600_000.0
            } ?: 0.0
            if (prior.lastReviewedAtEpochMs != null) {
                onlineModels.observeMemory(kcId, elapsedHours, prior.halfLifeHours, correct)
            }
            val baseUpdate = stateUpdater.update(component, prior, exercise, correct, confidence, latency, now, evidenceWeight)
            val particle = onlineModels.observeLatentState(
                kcId = kcId,
                priorMastery = prior.mastery,
                priorHalfLifeHours = prior.halfLifeHours,
                correct = correct,
                elapsedHours = elapsedHours
            )
            baseUpdate.copy(
                mastery = (0.78 * baseUpdate.mastery + 0.22 * particle.masteryMean).coerceIn(0.0, 1.0),
                halfLifeHours = (0.88 * baseUpdate.halfLifeHours + 0.12 * particle.halfLifeMean).coerceIn(0.25, 24.0 * 365.0),
                uncertainty = (0.75 * baseUpdate.uncertainty + 0.25 * (particle.masterySd * 2.5).coerceIn(0.04, 0.95)).coerceIn(0.04, 0.95)
            )
        }
        updatedStates.forEach { repository.updateLearnerState(it) }
        val afterMastery = primaryKcId?.let { id -> updatedStates.firstOrNull { it.kcId == id }?.mastery }
        if (primaryKcId != null) {
            val masteryGain = ((afterMastery ?: beforeMastery ?: 0.0) - (beforeMastery ?: 0.0)).coerceIn(-0.20, 0.20)
            val accuracyReward = if (correct) 0.70 else 0.0
            val speedReward = latency?.let { ms ->
                val target = (exercise.estimatedSeconds.coerceAtLeast(3) * 1000.0 * 0.30).coerceAtLeast(1200.0)
                (1.0 - (ms / (target * 2.0))).coerceIn(0.0, 1.0) * 0.15
            } ?: 0.05
            val learningReward = ((masteryGain + 0.20) / 0.40).coerceIn(0.0, 1.0) * 0.15
            onlineModels.observeStrategy(primaryKcId, exercise.type, (accuracyReward + speedReward + learningReward).coerceIn(0.0, 1.0))
        }
        repository.recordInteraction(
            InteractionRecord(
                sessionId = snap.sessionId ?: return,
                exerciseId = exercise.id,
                timestampEpochMs = now,
                response = response,
                correct = correct,
                confidence = confidence,
                latencyMs = latency,
                hintDepth = snap.helpDepth,
                plannerScore = activePlannerScore,
                stateBeforeMastery = beforeMastery,
                stateAfterMastery = afterMastery
            )
        )

        _snapshot.value = snap.copy(
            state = LessonState.FEEDBACK,
            completedItems = snap.completedItems + 1,
            correctItems = snap.correctItems + if (correct) 1 else 0,
            message = if (correct) "Correcto" else "Necesita ajuste"
        )
        earcon.play(if (correct) EarconKind.CORRECT else EarconKind.INCORRECT)
        val feedback = if (correct) {
            if (exercise.explanationEs.isNotBlank() && config.feedbackExplanations) "Correcto. ${exercise.explanationEs}" else "Correcto."
        } else {
            buildString {
                append("La respuesta correcta es ${exercise.correctOption.ifBlank { "la indicada" }}. ")
                if (exercise.explanationEs.isNotBlank()) append(exercise.explanationEs)
            }
        }
        speech.speak(feedback.trim(), "es-PE")
        if (_snapshot.value.completedItems >= config.maxItems) finishSession("Objetivo de sesión completado") else nextExercise()
    }

    private suspend fun nextExercise() {
        val current = _snapshot.value
        val now = clock.nowEpochMs()
        val learning = repository.loadSnapshot()
        val recent = repository.recentInteractions(80)
        val candidate = planner.choose(config.mode, learning, recent, now)
        if (candidate == null) {
            finishSession("No quedan actividades elegibles")
            return
        }
        activeExercise = candidate.exercise
        activePlannerScore = candidate.utility
        _snapshot.value = current.copy(
            state = LessonState.PREPARING,
            currentExerciseId = candidate.exercise.id,
            currentKcId = candidate.exercise.kcIds.firstOrNull(),
            lastPlannerRationale = candidate.rationale,
            helpDepth = 0,
            message = "Siguiente actividad"
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
            _snapshot.value = _snapshot.value.copy(state = LessonState.SPEAKING, message = "Explicación")
            earcon.play(EarconKind.NEW_PROMPT)
            if (exercise.promptEs.isNotBlank()) speech.speak(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speech.speak(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank()) {
                speech.speak("Se escribe", "es-PE")
                speech.speak(com.siaa.core.model.EnglishAlphabet.spellForSpeech(exercise.spellTarget), "en-US")
            }
            if (exercise.explanationEs.isNotBlank()) speech.speak(exercise.explanationEs, "es-PE")
            val snap = _snapshot.value
            val learning = repository.loadSnapshot()
            val now = clock.nowEpochMs()
            exercise.kcIds.forEach { kcId ->
                val component = learning.componentById[kcId] ?: return@forEach
                val prior = learning.stateByKcId[kcId] ?: LearnerKcState(kcId, mastery = component.priorMastery)
                repository.updateLearnerState(
                    prior.copy(
                        mastery = (prior.mastery + 0.04 * (1.0 - prior.mastery)).coerceAtMost(0.97),
                        recognition = (prior.recognition + 0.05).coerceAtMost(1.0),
                        halfLifeHours = (prior.halfLifeHours * 1.08).coerceAtMost(24.0 * 365.0),
                        lastReviewedAtEpochMs = now,
                        totalAttempts = prior.totalAttempts + 1
                    )
                )
            }
            val sessionId = snap.sessionId
            if (sessionId != null) {
                repository.recordInteraction(
                    InteractionRecord(
                        sessionId = sessionId,
                        exerciseId = exercise.id,
                        timestampEpochMs = clock.nowEpochMs(),
                        response = "TEACH_PRESENTED",
                        correct = true,
                        plannerScore = activePlannerScore
                    )
                )
            }
            _snapshot.value = snap.copy(completedItems = snap.completedItems + 1)
            nextExercise()
        }
        lastPrompt = prompt
        prompt()
    }

    private suspend fun presentBinary(exercise: ExerciseDefinition) {
        val prompt: suspend () -> Unit = {
            _snapshot.value = _snapshot.value.copy(state = LessonState.SPEAKING, message = "Pregunta")
            earcon.play(EarconKind.NEW_PROMPT)
            if (exercise.promptEs.isNotBlank()) speech.speak(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speech.speak(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank() && exercise.type == ExerciseType.SPELLING_AB) {
                speech.speak("Palabra objetivo: ${exercise.spellTarget}", "es-PE")
            }
            speech.speak("Opción A. ${exercise.optionA}. Opción B. ${exercise.optionB}.", optionLanguage(exercise))
            if (config.announceControls) speech.speak("Play pausa para A. Siguiente para B. Anterior para repetir.", "es-PE")
            _snapshot.value = _snapshot.value.copy(
                state = LessonState.WAITING_BINARY,
                promptFinishedAtMs = clock.nowEpochMs(),
                message = "Esperando A o B"
            )
        }
        lastPrompt = prompt
        prompt()
    }

    private suspend fun presentSelfAssessment(exercise: ExerciseDefinition) {
        val prompt: suspend () -> Unit = {
            _snapshot.value = _snapshot.value.copy(state = LessonState.SPEAKING, message = "Recuperación mental")
            earcon.play(EarconKind.NEW_PROMPT)
            if (exercise.promptEs.isNotBlank()) speech.speak(exercise.promptEs, "es-PE")
            if (exercise.stimulusEn.isNotBlank()) speech.speak(exercise.stimulusEn, "en-US")
            if (exercise.spellTarget.isNotBlank()) {
                speech.speak("Forma mentalmente el deletreo de ${exercise.spellTarget}.", "es-PE")
            }
            delay(2200)
            speech.speak("¿Lo resolviste? Play pausa para sí. Siguiente para dudé. Anterior para no.", "es-PE")
            _snapshot.value = _snapshot.value.copy(
                state = LessonState.WAITING_SELF_ASSESSMENT,
                promptFinishedAtMs = clock.nowEpochMs(),
                message = "Autoevaluación"
            )
        }
        lastPrompt = prompt
        prompt()
    }

    private fun optionLanguage(exercise: ExerciseDefinition): String = when (exercise.type) {
        ExerciseType.MEANING_AB -> "es-PE"
        else -> "en-US"
    }

    private suspend fun finishSession(message: String) {
        speech.stop()
        val snap = _snapshot.value
        snap.sessionId?.let { repository.finishSession(it, clock.nowEpochMs()) }
        _snapshot.value = snap.copy(state = LessonState.SESSION_END, message = message, promptFinishedAtMs = null)
        earcon.play(EarconKind.REGISTERED)
        speech.speak(message, "es-PE")
    }

    private fun fail(t: Throwable) {
        speech.stop()
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
