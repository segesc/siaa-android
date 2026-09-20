package com.siaa.core.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.siaa.core.runtime.SpeechPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class AndroidTtsSpeechPort(context: Context) : SpeechPort, TextToSpeech.OnInitListener {
    private val ready = CompletableDeferred<Unit>()
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val tts = TextToSpeech(context.applicationContext, this)

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                utteranceId ?: return
                val cont = pending.remove(utteranceId)
                if (cont != null && cont.isActive) cont.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId ?: return
                val cont = pending.remove(utteranceId)
                if (cont != null && cont.isActive) cont.resume(Unit)
            }
            override fun onError(utteranceId: String?, errorCode: Int) = onError(utteranceId)
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setSpeechRate(0.94f)
            if (!ready.isCompleted) ready.complete(Unit)
        } else if (!ready.isCompleted) {
            ready.completeExceptionally(IllegalStateException("TextToSpeech init failed: $status"))
        }
    }

    override suspend fun speak(text: String, languageTag: String, rate: Float) {
        if (text.isBlank()) return
        ready.await()
        tts.setSpeechRate((0.94f * rate).coerceIn(0.55f, 1.25f))
        val locale = Locale.forLanguageTag(languageTag)
        val availability = tts.isLanguageAvailable(locale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
        val id = UUID.randomUUID().toString()
        suspendCancellableCoroutine { cont ->
            pending[id] = cont
            cont.invokeOnCancellation {
                pending.remove(id)
                tts.stop()
            }
            val result = tts.speak(text, TextToSpeech.QUEUE_ADD, Bundle(), id)
            if (result == TextToSpeech.ERROR) {
                pending.remove(id)
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override fun stop() {
        tts.stop()
        val toCancel = pending.values.toList()
        pending.clear()
        toCancel.forEach { cont ->
            if (cont.isActive) {
                cont.cancel(CancellationException("TTS stopped"))
            }
        }
    }

    override fun shutdown() {
        stop()
        tts.shutdown()
    }
}

