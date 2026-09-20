package com.siaa.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaDiagnostics {
    private val _lastEvent = MutableStateFlow("Sin eventos")
    val lastEvent = _lastEvent.asStateFlow()

    private val _observedCommands = MutableStateFlow<Set<String>>(emptySet())
    val observedCommands = _observedCommands.asStateFlow()

    fun record(label: String, semanticCommand: String? = null) {
        _lastEvent.value = label
        if (semanticCommand != null) _observedCommands.value = _observedCommands.value + semanticCommand
    }

    fun clear() {
        _lastEvent.value = "Sin eventos"
        _observedCommands.value = emptySet()
    }
}
