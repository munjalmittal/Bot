package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.FloatingPopupActivity
import com.example.MainActivity
import com.example.R
import com.example.engine.ActionExecutor
import com.example.engine.AppManager
import com.example.engine.DeviceController
import com.example.engine.FileManager
import com.example.engine.SpeechManager
import com.example.model.WakeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BlackiBackgroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var speechManager: SpeechManager? = null
    private lateinit var appManager: AppManager
    private lateinit var fileManager: FileManager
    private lateinit var deviceController: DeviceController
    private lateinit var actionExecutor: ActionExecutor

    companion object {
        const val CHANNEL_ID = "blacki_background_voice_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, BlackiBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BlackiBackgroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        appManager = AppManager(this)
        fileManager = FileManager(this)
        deviceController = DeviceController(this)
        actionExecutor = ActionExecutor(appManager, fileManager, deviceController)

        // Seed study files in background if needed
        serviceScope.launch(Dispatchers.IO) {
            appManager.loadInstalledApps()
            fileManager.initializeAndSeedFiles()
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Blacki is listening silently in background"))

        initSpeechRecognition()
    }

    private fun initSpeechRecognition() {
        speechManager = SpeechManager(this) { recognizedText ->
            handleBackgroundVoiceCommand(recognizedText)
        }.apply {
            isContinuousListening = true
            startListening("en-IN")
        }
    }

    private fun handleBackgroundVoiceCommand(commandText: String) {
        serviceScope.launch {
            val config = WakeConfig(
                wakeWord = "Blacki",
                requireWakeWord = false, // Listen for direct commands or wake word
                silentMode = true
            )

            val outcome = actionExecutor.execute(
                input = commandText,
                wakeConfig = config,
                activity = null
            )

            // If a document / file was requested (e.g. "Blacki, open chapter 1 mathematics")
            // Open it in a floating popup view right over YouTube or whatever app the user is on!
            val fileToOpen = outcome.fileToPreview ?: if (outcome.result.type == com.example.model.ActionType.OPEN_FILE) {
                fileManager.findBestMatchingFileOrFolder(commandText)?.file
            } else null

            if (fileToOpen != null) {
                val popupIntent = Intent(this@BlackiBackgroundService, FloatingPopupActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("EXTRA_DOC_TITLE", fileToOpen.name)
                    putExtra("EXTRA_DOC_CONTENT", fileToOpen.previewText ?: "File opened")
                    putExtra("EXTRA_DOC_CATEGORY", fileToOpen.category)
                }
                startActivity(popupIntent)
            }

            // Update notification status briefly
            val noteManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            noteManager.notify(
                NOTIFICATION_ID,
                buildForegroundNotification("⚡ ${outcome.result.title}")
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Blacki Background Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Blacki active to listen for your commands over other apps."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Blacki Assistant Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY so Blacki stays alive even if closed from recents
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechManager?.destroy()
        serviceScope.cancel()
    }
}
