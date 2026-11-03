package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) {

    private val tag = "BlackiSpeech"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _statusText = MutableStateFlow("Ready for voice command")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    var isContinuousListening: Boolean = true
    private var currentLanguageTag: String = "en-IN"
    private var consecutiveErrors = 0

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            consecutiveErrors = 0
            _statusText.value = "Listening... Speak now (\"Blacki...\")"
            Log.d(tag, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            _statusText.value = "Hearing voice..."
            Log.d(tag, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical speech dB (-2 to 12) to 0.05..1.0
            val normalized = ((rmsdB + 2f) / 10f).coerceIn(0.05f, 1.0f)
            _audioRmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _statusText.value = "Analyzing command..."
            Log.d(tag, "onEndOfSpeech")
        }

        override fun onError(error: Int) {
            _audioRmsLevel.value = 0f
            _isListening.value = false
            consecutiveErrors++

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio hardware issue. Check tablet Mic toggle."
                SpeechRecognizer.ERROR_CLIENT -> "Client busy, reconnecting..."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission needed. Grant in Settings."
                SpeechRecognizer.ERROR_NETWORK -> "Network required for voice service."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Say \"Blacki, open yt\""
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Resetting microphone..."
                SpeechRecognizer.ERROR_SERVER -> "Voice server issue, retrying..."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening... say \"Blacki\""
                else -> "Voice recognition note ($error)"
            }

            Log.w(tag, "Speech recognition error: $error ($errorMsg)")
            _statusText.value = errorMsg

            // Auto-recover if continuous listening is on
            if (isContinuousListening && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                val delayMs = if (consecutiveErrors > 5) 2000L else 500L
                mainHandler.postDelayed({
                    startListening()
                }, delayMs)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioRmsLevel.value = 0f
            consecutiveErrors = 0

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val best = matches[0]
                _partialTranscript.value = best
                _statusText.value = "⚡ Got: \"$best\""
                Log.d(tag, "Recognized command: $best")
                onCommandRecognized(best)
            } else {
                _statusText.value = "Listening... (awaiting command)"
            }

            if (isContinuousListening) {
                mainHandler.postDelayed({
                    startListening()
                }, 400L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _partialTranscript.value = matches[0]
                _statusText.value = "Hearing: \"${matches[0]}\""
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun setLanguage(languageTag: String) {
        currentLanguageTag = languageTag
    }

    fun startListening(languageTag: String = currentLanguageTag) {
        currentLanguageTag = languageTag

        mainHandler.post {
            stopListeningInternal()

            try {
                // Try creating SpeechRecognizer directly
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(recognitionListener)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(
                        "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                        arrayOf("en-IN", "hi-IN", "en-US")
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _statusText.value = "Listening... Speak into tablet mic"
                Log.d(tag, "Started listening with locale $languageTag")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start speech recognizer: ${e.message}", e)
                _statusText.value = "Voice recognizer error: ${e.localizedMessage}. Tap 'System Voice Dialog' button."
                _isListening.value = false
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
            _isListening.value = false
            _audioRmsLevel.value = 0f
            _statusText.value = "Microphone paused. Tap to listen."
        }
    }

    fun destroy() {
        stopListening()
    }
}
