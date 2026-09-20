package com.siaa.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioFocusController(
    context: Context,
    private val onTransientLoss: () -> Unit,
    private val onGain: () -> Unit = {}
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onTransientLoss()
            AudioManager.AUDIOFOCUS_GAIN -> onGain()
        }
    }
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val request: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= 26) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(true)
            .build()
    } else null

    @Suppress("DEPRECATION")
    fun request(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= 26) {
            audioManager.requestAudioFocus(requireNotNull(request))
        } else {
            audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    fun abandon() {
        if (Build.VERSION.SDK_INT >= 26) {
            request?.let(audioManager::abandonAudioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(listener)
        }
    }
}
