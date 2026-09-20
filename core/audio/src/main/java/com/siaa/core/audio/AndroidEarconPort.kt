package com.siaa.core.audio

import android.media.AudioManager
import android.media.ToneGenerator
import com.siaa.core.model.EarconKind
import com.siaa.core.runtime.EarconPort

class AndroidEarconPort : EarconPort {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)

    override fun play(kind: EarconKind) {
        when (kind) {
            EarconKind.CORRECT -> tone.startTone(ToneGenerator.TONE_PROP_ACK, 140)
            EarconKind.INCORRECT -> tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            EarconKind.REGISTERED -> tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
            EarconKind.NEW_PROMPT -> tone.startTone(ToneGenerator.TONE_PROP_PROMPT, 70)
            EarconKind.WARNING -> tone.startTone(ToneGenerator.TONE_PROP_NACK, 260)
        }
    }

    override fun release() = tone.release()
}
