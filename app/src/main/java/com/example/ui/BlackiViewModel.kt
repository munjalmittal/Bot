package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ActionExecutor
import com.example.engine.AppManager
import com.example.engine.DeviceController
import com.example.engine.FileManager
import com.example.engine.SpeechManager
import com.example.engine.VolumeMode
import com.example.engine.BrightnessMode
import com.example.model.ActionResult
import com.example.model.BrightnessState
import com.example.model.IndexedFile
import com.example.model.InstalledApp
import com.example.model.SpeechLanguage
import com.example.model.VolumeState
import com.example.model.WakeConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlackiUiState(
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val audioRmsLevel: Float = 0f,
    val statusText: String = "Ready for command",
    val latestResult: ActionResult? = null,
    val commandHistory: List<ActionResult> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val indexedFiles: List<IndexedFile> = emptyList(),
    val wakeConfig: WakeConfig = WakeConfig(),
    val volumeState: VolumeState = VolumeState(10, 15, 66),
    val brightnessState: BrightnessState = BrightnessState(70, false),
    val isFlashlightOn: Boolean = false,
    val previewFile: IndexedFile? = null,
    val showScreenshotFlash: Boolean = false,
    val selectedTab: Int = 0,
    val detectedLanguage: String = "English / Hinglish",
    val isBackgroundServiceActive: Boolean = true
)

class BlackiViewModel(application: Application) : AndroidViewModel(application) {

    private val appManager = AppManager(application)
    private val fileManager = FileManager(application)
    private val deviceController = DeviceController(application)
    private val actionExecutor = ActionExecutor(appManager, fileManager, deviceController)

    private val _uiState = MutableStateFlow(BlackiUiState())
    val uiState: StateFlow<BlackiUiState> = _uiState.asStateFlow()

    private var speechManager: SpeechManager? = null

    init {
        // Initialize speech manager
        speechManager = SpeechManager(application) { recognizedText ->
            processCommand(recognizedText)
        }

        // Collect speech state
        viewModelScope.launch {
            speechManager?.isListening?.collect { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        }
        viewModelScope.launch {
            speechManager?.partialTranscript?.collect { text ->
                _uiState.update { it.copy(partialTranscript = text) }
            }
        }
        viewModelScope.launch {
            speechManager?.audioRmsLevel?.collect { level ->
                _uiState.update { it.copy(audioRmsLevel = level) }
            }
        }
        viewModelScope.launch {
            speechManager?.statusText?.collect { status ->
                _uiState.update { it.copy(statusText = status) }
            }
        }

        // Initialize state & seeded study files (Chapter 1 Mathematics, etc.)
        refreshDeviceState()
        viewModelScope.launch {
            val apps = appManager.loadInstalledApps()
            val files = fileManager.initializeAndSeedFiles()
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    indexedFiles = files
                )
            }
            try {
                com.example.service.BlackiBackgroundService.start(application)
            } catch (e: Exception) {
                // Handled if permission prompt is needed
            }
        }
    }

    fun toggleBackgroundService(enable: Boolean) {
        _uiState.update { it.copy(isBackgroundServiceActive = enable) }
        try {
            if (enable) {
                com.example.service.BlackiBackgroundService.start(getApplication())
            } else {
                com.example.service.BlackiBackgroundService.stop(getApplication())
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    fun refreshDeviceState() {
        val vol = deviceController.getVolumeState()
        val br = deviceController.getBrightnessState()
        val torch = deviceController.isFlashlightOn()
        _uiState.update {
            it.copy(
                volumeState = vol,
                brightnessState = br,
                isFlashlightOn = torch
            )
        }
    }

    fun processCommand(commandText: String, activity: Activity? = null) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(partialTranscript = trimmed, statusText = "⚡ Executing: $trimmed") }

            val outcome = actionExecutor.execute(
                input = trimmed,
                wakeConfig = _uiState.value.wakeConfig,
                activity = activity
            )

            refreshDeviceState()

            _uiState.update { state ->
                state.copy(
                    latestResult = outcome.result,
                    commandHistory = listOf(outcome.result) + state.commandHistory.take(25),
                    previewFile = outcome.fileToPreview,
                    showScreenshotFlash = outcome.triggerScreenshotFlash,
                    statusText = if (outcome.result.success) "⚡ Done in ${outcome.result.executionTimeMs}ms" else outcome.result.title
                )
            }

            if (outcome.triggerScreenshotFlash) {
                delay(600)
                _uiState.update { it.copy(showScreenshotFlash = false) }
            }
        }
    }

    fun toggleListening(hasMicPermission: Boolean) {
        if (!hasMicPermission) {
            _uiState.update { it.copy(statusText = "Microphone permission required") }
            return
        }
        val current = _uiState.value.isListening
        if (current) {
            speechManager?.stopListening()
        } else {
            speechManager?.startListening(_uiState.value.wakeConfig.language.localeTag)
        }
    }

    fun updateWakeWord(newWord: String) {
        val trimmed = newWord.trim().ifEmpty { "Blacki" }
        _uiState.update {
            it.copy(wakeConfig = it.wakeConfig.copy(wakeWord = trimmed))
        }
    }

    fun toggleRequireWakeWord(required: Boolean) {
        _uiState.update {
            it.copy(wakeConfig = it.wakeConfig.copy(requireWakeWord = required))
        }
    }

    fun setLanguage(language: SpeechLanguage) {
        _uiState.update {
            it.copy(wakeConfig = it.wakeConfig.copy(language = language))
        }
        speechManager?.setLanguage(language.localeTag)
    }

    fun toggleContinuousListening(enabled: Boolean) {
        _uiState.update {
            it.copy(wakeConfig = it.wakeConfig.copy(continuousListening = enabled))
        }
        speechManager?.isContinuousListening = enabled
        if (enabled && !_uiState.value.isListening) {
            speechManager?.startListening(_uiState.value.wakeConfig.language.localeTag)
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun addCustomAlias(alias: String, packageName: String) {
        appManager.addCustomAlias(alias, packageName)
        viewModelScope.launch {
            val updated = appManager.loadInstalledApps()
            _uiState.update { it.copy(installedApps = updated) }
        }
    }

    fun removeCustomAlias(alias: String) {
        appManager.removeCustomAlias(alias)
        viewModelScope.launch {
            val updated = appManager.loadInstalledApps()
            _uiState.update { it.copy(installedApps = updated) }
        }
    }

    fun launchAppDirectly(app: InstalledApp) {
        appManager.launchApp(app)
        deviceController.vibrateHaptic()
    }

    fun openFileDirectly(file: IndexedFile) {
        val outcome = fileManager.openFileOrFolder(file)
        if (!outcome.openedExternally) {
            _uiState.update { it.copy(previewFile = file) }
        }
        deviceController.vibrateHaptic()
    }

    fun hasAllFilesAccess(): Boolean = fileManager.hasAllFilesAccess()

    fun requestAllFilesAccess(activity: Activity?) {
        fileManager.requestAllFilesAccess(activity)
    }

    fun rescanLocalStorage() {
        viewModelScope.launch {
            val files = fileManager.initializeAndSeedFiles()
            _uiState.update { it.copy(indexedFiles = files) }
        }
    }

    fun createCustomNote(title: String, content: String) {
        val fileName = if (title.endsWith(".txt") || title.endsWith(".pdf")) title else "$title.txt"
        fileManager.createNewDocument(fileName, content, "User Notes")
        viewModelScope.launch {
            val updated = fileManager.initializeAndSeedFiles()
            _uiState.update { it.copy(indexedFiles = updated) }
        }
    }

    fun adjustVolumeDirect(mode: VolumeMode, percent: Int? = null) {
        val vol = deviceController.adjustVolume(mode, percent)
        _uiState.update { it.copy(volumeState = vol) }
    }

    fun adjustBrightnessDirect(activity: Activity?, mode: BrightnessMode, percent: Int? = null) {
        val br = deviceController.adjustBrightness(activity, mode, percent)
        _uiState.update { it.copy(brightnessState = br) }
    }

    fun toggleFlashlightDirect() {
        val state = deviceController.toggleFlashlight()
        _uiState.update { it.copy(isFlashlightOn = state) }
    }

    fun requestWriteSettingsPermission(activity: Activity?) {
        deviceController.requestWriteSettingsPermission(activity)
    }

    fun dismissFilePreview() {
        _uiState.update { it.copy(previewFile = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
