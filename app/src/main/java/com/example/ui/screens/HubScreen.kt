package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionResult
import com.example.ui.BlackiUiState
import com.example.ui.components.SonicOrb
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexGreen
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexSurfaceDark
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant

@Composable
fun HubScreen(
    uiState: BlackiUiState,
    isBackgroundServiceActive: Boolean,
    onOrbClick: () -> Unit,
    onLaunchSystemVoiceDialog: () -> Unit,
    onSendCommand: (String, Activity?) -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity
    var inputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Telemetry Status Bar
            TelemetryRow(
                volumePercent = uiState.volumeState.percentage,
                brightnessPercent = uiState.brightnessState.levelPercentage,
                isFlashlightOn = uiState.isFlashlightOn
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Central Sonic Voice Orb with Blacki Cat
        item {
            SonicOrb(
                isListening = uiState.isListening,
                audioRmsLevel = uiState.audioRmsLevel,
                statusText = uiState.statusText,
                wakeWord = uiState.wakeConfig.wakeWord,
                onOrbClick = onOrbClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time audio waveform level bars when listening
            if (uiState.isListening) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(VortexSurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mic Input Level:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val barCount = 7
                    for (i in 0 until barCount) {
                        val barHeight = (6 + (uiState.audioRmsLevel * 18 * ((i % 3) + 1) / 3)).coerceIn(4f, 24f)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (uiState.audioRmsLevel > 0.1f) VortexCyan else VortexPurple)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // System Voice Prompt Dialog Fallback Button
            androidx.compose.material3.OutlinedButton(
                onClick = onLaunchSystemVoiceDialog,
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = VortexCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VortexCyan.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("system_voice_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Google Voice Prompt",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Background Always-Listening Master Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("background_service_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isBackgroundServiceActive) VortexGreen.copy(alpha = 0.15f) else VortexSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isBackgroundServiceActive) VortexGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Background Listening",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isBackgroundServiceActive) VortexGreen else Color.Gray)
                                )
                            }
                            Text(
                                text = if (isBackgroundServiceActive)
                                    "Active over YouTube & all apps • Auto-boots"
                                else
                                    "Disabled • Tap switch to activate over other apps",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isBackgroundServiceActive,
                        onCheckedChange = { onToggleBackgroundService(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VortexDarkBg,
                            checkedTrackColor = VortexGreen,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = VortexSurfaceVariant
                        ),
                        modifier = Modifier.testTag("background_service_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Popup View Over Other Apps Indicator
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = VortexCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Multi-Window Display",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Over YouTube/Other apps: opens PDF/docs in Floating Popup View\n• From Home Screen: launches apps in Full Screen",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Command Input Field
        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Say or type command (\"Blacki, open chapter 1 mathematics\")...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VortexCyan,
                    unfocusedBorderColor = VortexSurfaceElevated,
                    focusedContainerColor = VortexSurfaceDark,
                    unfocusedContainerColor = VortexSurfaceDark,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = if (uiState.isListening) VortexCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendCommand(inputText, activity)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Command",
                            tint = VortexCyan
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onSendCommand(inputText, activity)
                        inputText = ""
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("command_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Latest Execution Card (Only shown when an action just executed)
        uiState.latestResult?.let { result ->
            item {
                LatestResultCard(result = result)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun TelemetryRow(
    volumePercent: Int,
    brightnessPercent: Int,
    isFlashlightOn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VortexSurfaceDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = VortexCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Vol: $volumePercent%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = VortexPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Screen: $brightnessPercent%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = if (isFlashlightOn) VortexGreen else Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isFlashlightOn) "Torch ON" else "Torch OFF", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun LatestResultCard(result: ActionResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("latest_result_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (result.success) VortexGreen else VortexPurple,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = result.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = VortexSurfaceElevated,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⚡ ${result.executionTimeMs} ms",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VortexCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.detail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Input: \"${result.rawQuery}\"",
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
