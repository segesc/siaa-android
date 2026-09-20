package com.siaa.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.MainViewModel
import com.siaa.core.model.KcDomain
import com.siaa.core.model.SessionMode
import com.siaa.core.runtime.LessonState

private enum class AppTab { HOME, PROGRESS, CURRICULUM, CALIBRATION, SETTINGS }

@Composable
fun SiaaApp(
    viewModel: MainViewModel,
    onStartMode: (SessionMode) -> Unit,
    onStop: () -> Unit,
    onStartCalibration: () -> Unit,
    onStopCalibration: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.HOME) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == AppTab.HOME, onClick = { tab = AppTab.HOME }, icon = {}, label = { Text("Inicio") })
                    NavigationBarItem(selected = tab == AppTab.PROGRESS, onClick = { tab = AppTab.PROGRESS }, icon = {}, label = { Text("Progreso") })
                    NavigationBarItem(selected = tab == AppTab.CURRICULUM, onClick = { tab = AppTab.CURRICULUM }, icon = {}, label = { Text("Mapa") })
                    NavigationBarItem(selected = tab == AppTab.CALIBRATION, onClick = { tab = AppTab.CALIBRATION }, icon = {}, label = { Text("Audífonos") })
                    NavigationBarItem(selected = tab == AppTab.SETTINGS, onClick = { tab = AppTab.SETTINGS }, icon = {}, label = { Text("Ajustes") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(state.contentReady, state.runtime, state.stats, onStartMode, onStop)
                    AppTab.PROGRESS -> ProgressScreen(state.states, state.sessions, viewModel::refresh)
                    AppTab.CURRICULUM -> CurriculumScreen(state.curriculum)
                    AppTab.CALIBRATION -> CalibrationScreen(
                        lastMediaEvent = state.lastMediaEvent,
                        observedCommands = state.observedCommands,
                        profile = state.deviceProfile,
                        onClear = viewModel::clearCalibration,
                        onSave = { viewModel.saveCalibration() },
                        onStartListening = onStartCalibration,
                        onStopListening = onStopCalibration
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        preferences = state.preferences,
                        onMaxItems = viewModel::setMaxItems,
                        onAnnounceControls = viewModel::setAnnounceControls,
                        onFeedbackExplanations = viewModel::setFeedbackExplanations
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    contentReady: Boolean,
    runtime: com.siaa.core.runtime.RuntimeSnapshot,
    stats: com.siaa.core.model.DashboardStats?,
    onStartMode: (SessionMode) -> Unit,
    onStop: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SIAA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Sistema de Inglés Auditivo Adaptativo")
        if (!contentReady) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Cargando currículo local…")
        }
        stats?.let {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Estado actual", fontWeight = FontWeight.SemiBold)
                    Text("Nivel estimado: ${it.currentCefrEstimate}")
                    Text("KCs dominados: ${it.masteredKcs}/${it.totalKcs}")
                    Text("Repasos debidos: ${it.dueKcs}")
                    Text("Dominio medio: ${(it.averageMastery * 100).toInt()} %")
                    Text("Interacciones registradas: ${it.totalInteractions}")
                }
            }
        }
        if (runtime.state !in setOf(LessonState.IDLE, LessonState.SESSION_END, LessonState.ERROR)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Sesión en curso", fontWeight = FontWeight.SemiBold)
                    Text("${runtime.mode} · ${runtime.message}")
                    Text("Completadas: ${runtime.completedItems} · correctas: ${runtime.correctItems}")
                    if (runtime.currentKcId != null) Text("KC: ${runtime.currentKcId}")
                    Button(onClick = onStop) { Text("Detener sesión") }
                }
            }
        }
        Text("Entrenamiento", style = MaterialTheme.typography.titleLarge)
        ModeButton("Sesión adaptativa", "Mezcla gramática, vocabulario, listening, ortografía y repasos.", SessionMode.ADAPTIVE, contentReady, onStartMode)
        ModeButton("Vocabulario", "Significado ↔ sonido ↔ escritura ↔ uso.", SessionMode.VOCABULARY, contentReady, onStartMode)
        ModeButton("Ortografía y deletreo", "Letras inglesas, spelling, patrones grafema-sonido.", SessionMode.SPELLING, contentReady, onStartMode)
        ModeButton("Gramática", "Reglas, contraste, construcción y transferencia.", SessionMode.GRAMMAR, contentReady, onStartMode)
        ModeButton("Listening", "Segmentación, comprensión y discriminación auditiva.", SessionMode.LISTENING, contentReady, onStartMode)
        ModeButton("Pronunciación", "Discriminación perceptiva y formas sonoras.", SessionMode.PRONUNCIATION, contentReady, onStartMode)
        HorizontalDivider()
        Text("Controles durante preguntas A/B", fontWeight = FontWeight.SemiBold)
        Text("Play/Pause = A · Siguiente = B · Anterior = repetir. En autoevaluación: Play/Pause = sí · Siguiente = dudé · Anterior = no.")
        Text("La sesión continúa con la pantalla apagada mediante MediaSessionService.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    mode: SessionMode,
    enabled: Boolean,
    onStartMode: (SessionMode) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onStartMode(mode) }, enabled = enabled) { Text("Iniciar") }
        }
    }
}

@Composable
private fun ProgressScreen(
    states: List<com.siaa.core.model.LearnerKcState>,
    sessions: List<com.siaa.core.model.SessionSummary>,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefresh) { Text("Actualizar") }
        }
        if (sessions.isNotEmpty()) {
            Text("Sesiones recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val fmt = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }
            sessions.take(8).forEach { session ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("${session.mode} · ${fmt.format(java.util.Date(session.startedAtEpochMs))}", fontWeight = FontWeight.Medium)
                        Text("${session.completedItems} actividades · ${(session.accuracy * 100).toInt()} % correctas" +
                            (session.meanLatencyMs?.let { " · latencia media ${(it / 1000.0).let { s -> "%.1f".format(s) }} s" } ?: ""),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            HorizontalDivider()
        }
        if (states.isEmpty()) Text("Aún no hay evidencia suficiente.")
        Text("Componentes de conocimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        states.sortedByDescending { it.totalAttempts }.take(80).forEach { s ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.kcId, fontWeight = FontWeight.Medium)
                    Text("Dominio ${(s.mastery * 100).toInt()} % · retención h½ ${"%.1f".format(s.halfLifeHours)} h")
                    Text("Reconocimiento ${(s.recognition * 100).toInt()} % · producción ${(s.production * 100).toInt()} % · ortografía ${(s.orthography * 100).toInt()} %")
                    Text("Intentos ${s.totalAttempts} · aciertos ${s.totalCorrect}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CurriculumScreen(items: List<com.siaa.app.CurriculumItem>) {
    var level by remember { mutableStateOf("Todos") }
    var domain by remember { mutableStateOf("Todos") }
    val levels = listOf("Todos", "Pre-A1", "A1", "A2", "B1", "B2", "C1", "C2")
    val domains = listOf("Todos") + KcDomain.entries.map { it.name }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mapa curricular", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Cada KC muestra dominio, readiness y si sus prerrequisitos duros permiten trabajarlo.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var levelOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { levelOpen = true }) { Text(level) }
                DropdownMenu(expanded = levelOpen, onDismissRequest = { levelOpen = false }) {
                    levels.forEach { x -> DropdownMenuItem(text = { Text(x) }, onClick = { level = x; levelOpen = false }) }
                }
            }
            var domainOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { domainOpen = true }) { Text(domain) }
                DropdownMenu(expanded = domainOpen, onDismissRequest = { domainOpen = false }) {
                    domains.forEach { x -> DropdownMenuItem(text = { Text(x) }, onClick = { domain = x; domainOpen = false }) }
                }
            }
        }
        items.asSequence()
            .filter { level == "Todos" || it.cefr.equals(level, true) }
            .filter { domain == "Todos" || it.domain.name == domain }
            .take(160)
            .forEach { item ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text("${item.cefr} · ${item.domain.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(progress = { item.mastery.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Dominio ${(item.mastery * 100).toInt()} % · readiness ${(item.readiness * 100).toInt()} % · ${if (item.unlocked) "desbloqueado" else "bloqueado"}", style = MaterialTheme.typography.bodySmall)
                        Text(item.id, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
    }
}

@Composable
private fun CalibrationScreen(
    lastMediaEvent: String,
    observedCommands: Set<String>,
    profile: com.siaa.core.model.DeviceProfile?,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Calibración de audífonos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Pulsa ‘Escuchar controles’ y luego prueba cada gesto de tus audífonos. Android no entrega el número de taps físico; entrega comandos multimedia generados por el firmware.")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Último evento recibido", fontWeight = FontWeight.SemiBold)
                Text(lastMediaEvent)
                Text("Comandos observados: ${observedCommands.sorted().joinToString().ifBlank { "ninguno" }}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartListening) { Text("Escuchar controles") }
            OutlinedButton(onClick = onStopListening) { Text("Detener escucha") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = observedCommands.isNotEmpty()) { Text("Guardar perfil") }
            OutlinedButton(onClick = onClear) { Text("Limpiar") }
        }
        profile?.let {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Perfil guardado", fontWeight = FontWeight.SemiBold)
                    Text(it.name)
                    Text("Play/Pause: ${it.playPauseAvailable} · Next: ${it.nextAvailable} · Previous: ${it.previousAvailable}")
                }
            }
        }
        Text("Objetivo mínimo: Play/Pause y Next. Previous mejora la experiencia porque permite repetir o marcar ‘no’.")
        Text("Si tu modelo reasigna doble/triple toque desde su app del fabricante, configúralo para exponer Play/Pause, Next y Previous.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(
    preferences: com.siaa.app.UserPreferences,
    onMaxItems: (Int) -> Unit,
    onAnnounceControls: (Boolean) -> Unit,
    onFeedbackExplanations: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sesión auditiva", fontWeight = FontWeight.SemiBold)
                Text("Máximo de actividades: ${preferences.maxItems}")
                Slider(
                    value = preferences.maxItems.toFloat(),
                    onValueChange = { onMaxItems(it.toInt()) },
                    valueRange = 10f..150f,
                    steps = 13
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Anunciar controles")
                    Switch(checked = preferences.announceControls, onCheckedChange = onAnnounceControls)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Explicación tras respuesta")
                    Switch(checked = preferences.feedbackExplanations, onCheckedChange = onFeedbackExplanations)
                }
            }
        }
        Text("Arquitectura", style = MaterialTheme.typography.titleLarge)
        SettingCard("Offline-first", "El grafo, modelo del alumno, planificador, contenido y base Room funcionan localmente.")
        SettingCard("Audio", "TTS local de Android + earcons. El motor puede sustituirse luego por clips pregrabados o TTS neural sin cambiar el runtime.")
        SettingCard("Planificador", "Utility planner + aproximación POMDP, diagnóstico por Q-matrix, olvido y remediación por wheel-spinning.")
        SettingCard("Privacidad", "Las interacciones se almacenan localmente en siaa.db. No existe telemetría remota en esta versión.")
        SettingCard("Contenido", "El content pack se valida antes de insertarse: referencias de KCs, dificultad y ciclos de prerrequisitos.")
    }
}

@Composable
private fun SettingCard(title: String, text: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text)
        }
    }
}
