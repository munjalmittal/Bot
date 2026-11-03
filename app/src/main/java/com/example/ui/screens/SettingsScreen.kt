package com.example.ui.screens

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeechLanguage
import com.example.model.WakeConfig
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexGreen
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexSurfaceDark
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant

@Composable
fun SettingsScreen(
    wakeConfig: WakeConfig,
    hasMicPermission: Boolean,
    onUpdateWakeWord: (String) -> Unit,
    onToggleRequireWakeWord: (Boolean) -> Unit,
    onSelectLanguage: (SpeechLanguage) -> Unit,
    onRequestWriteSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customWakeInput by remember(wakeConfig.wakeWord) { mutableStateOf(wakeConfig.wakeWord) }
    val presetWakeWords = listOf("Blacki", "Blackie", "ब्लैकी", "Nova", "Jarvis")
    val context = LocalContext.current
    val hasSystemWrite = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ASSISTANT CONFIGURATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = VortexCyan
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Wake Word Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = VortexCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wake Name",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Name your assistant. You can call \"Blacki, open youtube\" or choose any nickname.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetWakeWords.forEach { preset ->
                        val isSelected = wakeConfig.wakeWord.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                customWakeInput = preset
                                onUpdateWakeWord(preset)
                            },
                            label = { Text(preset, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VortexCyan,
                                selectedLabelColor = VortexDarkBg,
                                containerColor = VortexSurfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customWakeInput,
                    onValueChange = {
                        customWakeInput = it
                        if (it.isNotBlank()) onUpdateWakeWord(it)
                    },
                    label = { Text("Custom Wake Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_wake_word_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Require Wake Word Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require Wake Name Prefix",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (wakeConfig.requireWakeWord) "Must say \"${wakeConfig.wakeWord}\" before every command" else "Disabled: commands like \"yt kholo\" execute immediately",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = wakeConfig.requireWakeWord,
                        onCheckedChange = { onToggleRequireWakeWord(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VortexCyan,
                            checkedTrackColor = VortexCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("require_wake_word_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language & Dialect Selection
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Speech & Natural Language",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Blacki understands Hindi, Hinglish, and English commands seamlessly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                SpeechLanguage.values().forEach { lang ->
                    val isSelected = wakeConfig.language == lang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VortexSurfaceVariant else VortexSurfaceDark)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = lang.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) VortexCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Locale: ${lang.localeTag}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onSelectLanguage(lang) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) VortexCyan else VortexSurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSelected) "Active" else "Select",
                                fontSize = 11.sp,
                                color = if (isSelected) VortexDarkBg else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Silent Execution Mode Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = null,
                    tint = VortexGreen,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Silent Execution Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active: Blacki does his work silently without voice speaking back. Sub-second execution with visual HUD confirmation.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Permissions Dashboard
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VortexSurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Permissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                PermissionStatusItem(
                    title = "Microphone",
                    description = "Required for listening to \"Blacki\" voice commands",
                    isGranted = hasMicPermission,
                    icon = Icons.Default.Mic
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionStatusItem(
                    title = "System Brightness Write",
                    description = "Allows adjusting global display brightness via voice",
                    isGranted = hasSystemWrite,
                    icon = Icons.Default.BrightnessMedium,
                    actionButton = if (!hasSystemWrite) {
                        {
                            Button(
                                onClick = onRequestWriteSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = VortexPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp)
                            }
                        }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionStatusItem(
                    title = "Flashlight / Camera",
                    description = "Allows toggling hardware torch (\"torch jalao\")",
                    isGranted = true,
                    icon = Icons.Default.FlashlightOn
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun PermissionStatusItem(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionButton: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VortexSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) VortexGreen else VortexPurple,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) VortexGreen else VortexPurple,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (actionButton != null) {
            actionButton()
        }
    }
}
