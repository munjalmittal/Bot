package com.example.engine

import android.app.Activity
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.IndexedFile
import com.example.model.WakeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionExecutor(
    private val appManager: AppManager,
    private val fileManager: FileManager,
    private val deviceController: DeviceController
) {

    data class ExecutionOutcome(
        val result: ActionResult,
        val fileToPreview: IndexedFile? = null,
        val triggerScreenshotFlash: Boolean = false
    )

    suspend fun execute(
        input: String,
        wakeConfig: WakeConfig,
        activity: Activity? = null
    ): ExecutionOutcome = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val parsed = ActionParser.parse(
            input = input,
            configuredWakeWord = wakeConfig.wakeWord,
            requireWakeWord = wakeConfig.requireWakeWord
        )

        if (wakeConfig.requireWakeWord && !parsed.wakeWordFound) {
            val latency = System.currentTimeMillis() - startTime
            return@withContext ExecutionOutcome(
                result = ActionResult(
                    success = false,
                    title = "Wake Word Required",
                    detail = "Say \"${wakeConfig.wakeWord}, [command]\" (or disable wake word requirement in Settings)",
                    type = ActionType.UNKNOWN,
                    executionTimeMs = latency,
                    rawQuery = input
                )
            )
        }

        val outcome = when (val intent = parsed.intent) {
            is ParsedIntent.OpenApp -> {
                val appMatch = appManager.findApp(intent.targetQuery)
                if (appMatch != null) {
                    val launched = appManager.launchApp(appMatch.app)
                    val latency = System.currentTimeMillis() - startTime
                    ExecutionOutcome(
                        result = ActionResult(
                            success = launched,
                            title = "Opened ${appMatch.app.label}",
                            detail = "${appMatch.matchType} (${appMatch.app.packageName})",
                            type = ActionType.OPEN_APP,
                            executionTimeMs = latency,
                            rawQuery = input
                        )
                    )
                } else {
                    // Check local storage files or folders BEFORE anything else!
                    val localMatch = fileManager.findBestMatchingFileOrFolder(intent.targetQuery)
                    if (localMatch != null) {
                        val openOutcome = fileManager.openFileOrFolder(localMatch.file)
                        val latency = System.currentTimeMillis() - startTime
                        val itemType = if (localMatch.file.isDirectory) "Local Folder" else "Local File"
                        ExecutionOutcome(
                            result = ActionResult(
                                success = true,
                                title = "Opened ${localMatch.file.name}",
                                detail = "${localMatch.reason} • $itemType (${localMatch.file.path})",
                                type = ActionType.OPEN_FILE,
                                executionTimeMs = latency,
                                rawQuery = input,
                                extraData = localMatch.file.previewText
                            ),
                            fileToPreview = if (!openOutcome.openedExternally) localMatch.file else null
                        )
                    } else if (isExplicitWebService(intent.targetQuery)) {
                        // Only try web for explicit online services like YouTube, Claude, ChatGPT
                        val webLaunched = appManager.launchWebFallback(intent.targetQuery)
                        val latency = System.currentTimeMillis() - startTime
                        ExecutionOutcome(
                            result = ActionResult(
                                success = webLaunched,
                                title = "Opened ${intent.targetQuery.capitalize()} (Web)",
                                detail = "Launched web app",
                                type = ActionType.OPEN_APP,
                                executionTimeMs = latency,
                                rawQuery = input
                            )
                        )
                    } else {
                        val latency = System.currentTimeMillis() - startTime
                        ExecutionOutcome(
                            result = ActionResult(
                                success = false,
                                title = "Not Found Locally",
                                detail = "No local app, file, or folder found matching '${intent.targetQuery}' in storage.",
                                type = ActionType.OPEN_FILE,
                                executionTimeMs = latency,
                                rawQuery = input
                            )
                        )
                    }
                }
            }

            is ParsedIntent.OpenFile -> {
                val match = fileManager.findBestMatchingFileOrFolder(intent.targetFileNameQuery)
                val latency = System.currentTimeMillis() - startTime
                if (match != null) {
                    val openOutcome = fileManager.openFileOrFolder(match.file)
                    val itemType = if (match.file.isDirectory) "Folder" else "File"
                    ExecutionOutcome(
                        result = ActionResult(
                            success = true,
                            title = "Opened ${match.file.name}",
                            detail = "${match.reason} • $itemType (${match.file.path})",
                            type = ActionType.OPEN_FILE,
                            executionTimeMs = latency,
                            rawQuery = input,
                            extraData = match.file.previewText
                        ),
                        fileToPreview = if (!openOutcome.openedExternally) match.file else null
                    )
                } else {
                    ExecutionOutcome(
                        result = ActionResult(
                            success = false,
                            title = "Local File or Folder Not Found",
                            detail = "No match for '${intent.targetFileNameQuery}' in tablet storage.",
                            type = ActionType.OPEN_FILE,
                            executionTimeMs = latency,
                            rawQuery = input
                        )
                    )
                }
            }

            is ParsedIntent.AdjustVolume -> {
                val volState = deviceController.adjustVolume(intent.mode, intent.targetPercent)
                val latency = System.currentTimeMillis() - startTime
                val title = when (intent.mode) {
                    VolumeMode.MUTE -> "Volume Muted"
                    VolumeMode.UNMUTE -> "Volume Restored (${volState.percentage}%)"
                    VolumeMode.MAX -> "Max Volume (100%)"
                    VolumeMode.UP -> "Volume Increased to ${volState.percentage}%"
                    VolumeMode.DOWN -> "Volume Decreased to ${volState.percentage}%"
                    VolumeMode.SET_PERCENT -> "Volume Set to ${volState.percentage}%"
                }
                ExecutionOutcome(
                    result = ActionResult(
                        success = true,
                        title = title,
                        detail = "Level: ${volState.currentVolume} / ${volState.maxVolume} (${volState.percentage}%)",
                        type = ActionType.VOLUME_CONTROL,
                        executionTimeMs = latency,
                        rawQuery = input
                    )
                )
            }

            is ParsedIntent.AdjustBrightness -> {
                val brState = deviceController.adjustBrightness(activity, intent.mode, intent.targetPercent)
                val latency = System.currentTimeMillis() - startTime
                val title = when (intent.mode) {
                    BrightnessMode.MAX -> "Max Brightness (100%)"
                    BrightnessMode.MIN -> "Lowest Brightness (10%)"
                    BrightnessMode.UP -> "Brightness Increased to ${brState.levelPercentage}%"
                    BrightnessMode.DOWN -> "Brightness Decreased to ${brState.levelPercentage}%"
                    BrightnessMode.SET_PERCENT -> "Brightness Set to ${brState.levelPercentage}%"
                }
                ExecutionOutcome(
                    result = ActionResult(
                        success = true,
                        title = title,
                        detail = if (brState.hasSystemWritePermission) {
                            "Applied to Screen & System Settings (${brState.levelPercentage}%)"
                        } else {
                            "Applied to App Window (${brState.levelPercentage}%)"
                        },
                        type = ActionType.BRIGHTNESS_CONTROL,
                        executionTimeMs = latency,
                        rawQuery = input
                    )
                )
            }

            is ParsedIntent.TakeScreenshot -> {
                deviceController.vibrateHaptic()
                val latency = System.currentTimeMillis() - startTime
                ExecutionOutcome(
                    result = ActionResult(
                        success = true,
                        title = "Screenshot Captured",
                        detail = "Screen grab recorded and copied to clipboard",
                        type = ActionType.SCREENSHOT,
                        executionTimeMs = latency,
                        rawQuery = input
                    ),
                    triggerScreenshotFlash = true
                )
            }

            is ParsedIntent.ToggleFlashlight -> {
                val state = deviceController.toggleFlashlight(intent.enable)
                val latency = System.currentTimeMillis() - startTime
                ExecutionOutcome(
                    result = ActionResult(
                        success = true,
                        title = "Flashlight ${if (state) "Turned ON" else "Turned OFF"}",
                        detail = "Torch hardware ${if (state) "active" else "disabled"}",
                        type = ActionType.FLASHLIGHT,
                        executionTimeMs = latency,
                        rawQuery = input
                    )
                )
            }

            is ParsedIntent.OpenSettings -> {
                deviceController.openSystemSettings(intent.screenType)
                val latency = System.currentTimeMillis() - startTime
                ExecutionOutcome(
                    result = ActionResult(
                        success = true,
                        title = "Opened ${intent.screenType.capitalize()} Settings",
                        detail = "System panel launched",
                        type = ActionType.SETTINGS_TOGGLE,
                        executionTimeMs = latency,
                        rawQuery = input
                    )
                )
            }

            is ParsedIntent.WebSearch -> {
                val launched = appManager.launchWebFallback(intent.query)
                val latency = System.currentTimeMillis() - startTime
                ExecutionOutcome(
                    result = ActionResult(
                        success = launched,
                        title = "Searched \"${intent.query}\"",
                        detail = "Opened web search results",
                        type = ActionType.WEB_SEARCH,
                        executionTimeMs = latency,
                        rawQuery = input
                    )
                )
            }

            ParsedIntent.Unknown -> {
                val match = appManager.findApp(parsed.cleanedCommand)
                val latency = System.currentTimeMillis() - startTime
                if (match != null) {
                    val launched = appManager.launchApp(match.app)
                    ExecutionOutcome(
                        result = ActionResult(
                            success = launched,
                            title = "Opened ${match.app.label}",
                            detail = "Inferred intent: ${match.matchType}",
                            type = ActionType.OPEN_APP,
                            executionTimeMs = latency,
                            rawQuery = input
                        )
                    )
                } else {
                    // Check if query matches a local file or folder
                    val localItem = fileManager.findBestMatchingFileOrFolder(parsed.cleanedCommand)
                    if (localItem != null) {
                        val openOutcome = fileManager.openFileOrFolder(localItem.file)
                        val itemType = if (localItem.file.isDirectory) "Local Folder" else "Local File"
                        ExecutionOutcome(
                            result = ActionResult(
                                success = true,
                                title = "Opened ${localItem.file.name}",
                                detail = "${localItem.reason} • $itemType (${localItem.file.path})",
                                type = ActionType.OPEN_FILE,
                                executionTimeMs = latency,
                                rawQuery = input,
                                extraData = localItem.file.previewText
                            ),
                            fileToPreview = if (!openOutcome.openedExternally) localItem.file else null
                        )
                    } else {
                        ExecutionOutcome(
                            result = ActionResult(
                                success = false,
                                title = "Unrecognized Local Command",
                                detail = "Could not find '${parsed.cleanedCommand}' in apps, files, or folders.",
                                type = ActionType.UNKNOWN,
                                executionTimeMs = latency,
                                rawQuery = input
                            )
                        )
                    }
                }
            }
        }

        return@withContext outcome
    }

    private fun isExplicitWebService(query: String): Boolean {
        val q = query.lowercase()
        return q == "youtube" || q == "yt" || q == "claude" || q == "chatgpt" || q == "gpt" || q == "spotify"
    }

    private fun String.capitalize(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
