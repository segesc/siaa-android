package com.siaa.app.media

import android.content.Intent
import android.view.KeyEvent
import com.siaa.core.runtime.RuntimeCommand

class EarbudCommandRouter {
    fun fromMediaButtonIntent(intent: Intent): Pair<RuntimeCommand, Int>? {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return null
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        val command = when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> RuntimeCommand.PRIMARY
            KeyEvent.KEYCODE_MEDIA_PLAY -> RuntimeCommand.PRIMARY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> RuntimeCommand.PAUSE
            KeyEvent.KEYCODE_MEDIA_NEXT -> RuntimeCommand.SECONDARY
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> RuntimeCommand.BACK
            KeyEvent.KEYCODE_MEDIA_STOP -> RuntimeCommand.STOP
            else -> return null
        }
        return command to event.keyCode
    }
}
