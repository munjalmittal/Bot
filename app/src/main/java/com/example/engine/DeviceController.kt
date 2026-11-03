package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.model.BrightnessState
import com.example.model.VolumeState

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchOn = false

    // Track window brightness level (0% to 100%)
    private var currentBrightnessPercent = 70

    fun getVolumeState(): VolumeState {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = if (max > 0) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0
        return VolumeState(currentVolume = current, maxVolume = max, percentage = percent)
    }

    fun adjustVolume(mode: VolumeMode, targetPercent: Int? = null): VolumeState {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        when (mode) {
            VolumeMode.UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            VolumeMode.DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            VolumeMode.MUTE -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            }
            VolumeMode.UNMUTE -> {
                val half = (max / 2).coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, half, AudioManager.FLAG_SHOW_UI)
            }
            VolumeMode.MAX -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
            }
            VolumeMode.SET_PERCENT -> {
                val pct = (targetPercent ?: 50).coerceIn(0, 100)
                val newVol = ((pct / 100f) * max).toInt().coerceIn(0, max)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
            }
        }
        vibrateHaptic()
        return getVolumeState()
    }

    fun getBrightnessState(): BrightnessState {
        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
        return BrightnessState(
            levelPercentage = currentBrightnessPercent,
            hasSystemWritePermission = canWrite
        )
    }

    fun adjustBrightness(
        activity: Activity?,
        mode: BrightnessMode,
        targetPercent: Int? = null
    ): BrightnessState {
        when (mode) {
            BrightnessMode.UP -> {
                currentBrightnessPercent = (currentBrightnessPercent + 20).coerceAtMost(100)
            }
            BrightnessMode.DOWN -> {
                currentBrightnessPercent = (currentBrightnessPercent - 20).coerceAtLeast(10)
            }
            BrightnessMode.MAX -> {
                currentBrightnessPercent = 100
            }
            BrightnessMode.MIN -> {
                currentBrightnessPercent = 10
            }
            BrightnessMode.SET_PERCENT -> {
                currentBrightnessPercent = (targetPercent ?: 50).coerceIn(10, 100)
            }
        }

        // Apply immediately to the current window
        activity?.let { act ->
            act.runOnUiThread {
                val layoutParams = act.window.attributes
                layoutParams.screenBrightness = (currentBrightnessPercent / 100f).coerceIn(0.05f, 1.0f)
                act.window.attributes = layoutParams
            }
        }

        // Also attempt system write if permitted
        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true

        if (canWrite) {
            try {
                val sysValue = ((currentBrightnessPercent / 100f) * 255).toInt().coerceIn(10, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    sysValue
                )
            } catch (e: Exception) {
                // Ignore fallback
            }
        }

        vibrateHaptic()
        return BrightnessState(currentBrightnessPercent, canWrite)
    }

    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        if (cameraManager == null) return false
        val newState = forceState ?: !isTorchOn
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            vibrateHaptic()
            newState
        } catch (e: Exception) {
            false
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun openSystemSettings(type: String) {
        val intent = when (type.lowercase()) {
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        vibrateHaptic()
    }

    fun requestWriteSettingsPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    fun vibrateHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            // Non-critical
        }
    }
}
