package com.example.model

import android.graphics.drawable.Drawable
import android.net.Uri

enum class ActionType {
    OPEN_APP,
    OPEN_FILE,
    VOLUME_CONTROL,
    BRIGHTNESS_CONTROL,
    SCREENSHOT,
    FLASHLIGHT,
    SETTINGS_TOGGLE,
    WEB_SEARCH,
    UNKNOWN
}

data class ActionResult(
    val id: String = System.currentTimeMillis().toString(),
    val success: Boolean,
    val title: String,
    val detail: String,
    val type: ActionType,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val rawQuery: String,
    val extraData: String? = null
)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val aliases: List<String> = emptyList(),
    val launchCount: Int = 0
)

data class IndexedFile(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri,
    val mimeType: String,
    val sizeBytes: Long,
    val category: String,
    val previewText: String? = null,
    val isDirectory: Boolean = false
)

enum class SpeechLanguage(val displayName: String, val localeTag: String) {
    AUTO_HINGLISH("Hinglish & English (India)", "en-IN"),
    HINDI("हिंदी (Hindi)", "hi-IN"),
    ENGLISH_US("English (US)", "en-US")
}

data class WakeConfig(
    val wakeWord: String = "Blacki",
    val requireWakeWord: Boolean = false, // false by default allows instant direct commands or wake word
    val silentMode: Boolean = true, // User requested: "he should not speak but he should do his work well"
    val continuousListening: Boolean = false,
    val language: SpeechLanguage = SpeechLanguage.AUTO_HINGLISH
)

data class VolumeState(
    val currentVolume: Int,
    val maxVolume: Int,
    val percentage: Int
)

data class BrightnessState(
    val levelPercentage: Int,
    val hasSystemWritePermission: Boolean
)
