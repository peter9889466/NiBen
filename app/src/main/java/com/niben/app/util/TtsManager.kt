package com.niben.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class TtsManager(context: Context) {
    private var textToSpeech: TextToSpeech? = null
    var isReady: Boolean = false
        private set

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("TtsManager", "Japanese TTS language is not supported or missing data")
                    isReady = false
                } else {
                    textToSpeech?.setSpeechRate(0.85f) // 학습자에게 적합한 또렷한 속도
                    isReady = true
                }
            } else {
                Log.e("TtsManager", "TTS Initialization failed: status=$status")
                isReady = false
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (isReady && textToSpeech != null) {
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "NiBen_TTS_${System.currentTimeMillis()}"
            )
        }
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w("TtsManager", "Error shutting down TTS", e)
        } finally {
            textToSpeech = null
            isReady = false
        }
    }
}

/**
 * Compose 라이프사이클에 맞추어 TtsManager 인스턴스를 생성하고 소멸 시 안전하게 shutdown()을 호출하는 Composable 헬퍼
 */
@Composable
fun rememberTtsManager(): TtsManager {
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }

    DisposableEffect(ttsManager) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    return ttsManager
}
