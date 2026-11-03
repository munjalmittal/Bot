package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.SpeechLanguage
import com.example.ui.components.DocumentViewerDialog
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexGreen
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexSurfaceDark
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackiApp(
    viewModel: BlackiViewModel,
    activity: Activity? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleListening(true)
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!spoken.isNullOrEmpty()) {
                viewModel.processCommand(spoken[0], activity)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = VortexDarkBg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Black Cat Logo Avatar with warm yellow border
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDE68A))
                                    .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.blacki_cat_logo),
                                    contentDescription = "Blacki Cat Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "BLACKI",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = VortexCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "SUB-SECOND",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VortexCyan,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Silent Voice Action Copilot • Hindi & English",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick sub-second speed badge
                        Surface(
                            color = VortexSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = VortexGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "< 40ms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VortexGreen
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VortexDarkBg
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = VortexSurfaceDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Mic, contentDescription = "Hub") },
                        label = { Text("Hub", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VortexDarkBg,
                            selectedTextColor = VortexCyan,
                            indicatorColor = VortexCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                        label = { Text("Apps", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VortexDarkBg,
                            selectedTextColor = VortexCyan,
                            indicatorColor = VortexCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Files") },
                        label = { Text("Study Files", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VortexDarkBg,
                            selectedTextColor = VortexCyan,
                            indicatorColor = VortexCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VortexDarkBg,
                            selectedTextColor = VortexCyan,
                            indicatorColor = VortexCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedTab) {
                    0 -> HubScreen(
                        uiState = uiState,
                        isBackgroundServiceActive = uiState.isBackgroundServiceActive,
                        onOrbClick = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleListening(true)
                            }
                        },
                        onLaunchSystemVoiceDialog = {
                            if (!hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                try {
                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, uiState.wakeConfig.language.localeTag)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak command to Blacki...")
                                    }
                                    systemSpeechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    viewModel.toggleListening(true)
                                }
                            }
                        },
                        onSendCommand = { cmd, act ->
                            viewModel.processCommand(cmd, act)
                        },
                        onToggleBackgroundService = { enable ->
                            viewModel.toggleBackgroundService(enable)
                        },
                        onLanguageClick = {
                            viewModel.selectTab(3)
                        }
                    )

                    1 -> AppsScreen(
                        apps = uiState.installedApps,
                        onLaunchApp = { app -> viewModel.launchAppDirectly(app) },
                        onAddAlias = { alias, pkg -> viewModel.addCustomAlias(alias, pkg) }
                    )

                    2 -> FilesScreen(
                        files = uiState.indexedFiles,
                        hasAllFilesAccess = viewModel.hasAllFilesAccess(),
                        onOpenFile = { file -> viewModel.openFileDirectly(file) },
                        onRequestAllFilesAccess = { viewModel.requestAllFilesAccess(activity) },
                        onRescanStorage = { viewModel.rescanLocalStorage() },
                        onCreateNewDoc = { title, content -> viewModel.createCustomNote(title, content) }
                    )

                    3 -> SettingsScreen(
                        wakeConfig = uiState.wakeConfig,
                        hasMicPermission = hasMicPermission,
                        onUpdateWakeWord = { viewModel.updateWakeWord(it) },
                        onToggleRequireWakeWord = { viewModel.toggleRequireWakeWord(it) },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onRequestWriteSettings = { viewModel.requestWriteSettingsPermission(activity) }
                    )
                }
            }
        }

        // Animated Screenshot Flash Effect
        AnimatedVisibility(
            visible = uiState.showScreenshotFlash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // In-app Document Viewer Dialog (for Chapter 1 Mathematics and study files)
        uiState.previewFile?.let { file ->
            DocumentViewerDialog(
                file = file,
                onDismiss = { viewModel.dismissFilePreview() }
            )
        }
    }
}
