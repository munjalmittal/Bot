package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant

data class QuickActionItem(
    val label: String,
    val command: String,
    val icon: ImageVector,
    val isHinglish: Boolean = false
)

@Composable
fun QuickActionChips(
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hinglishActions = listOf(
        QuickActionItem("yt kholo", "yt kholo", Icons.Default.PlayArrow, true),
        QuickActionItem("chater 1 math kholo", "chapter 1 mathematics kholo", Icons.Default.Description, true),
        QuickActionItem("awaz badhao", "awaz badhao", Icons.Default.VolumeUp, true),
        QuickActionItem("screenshot lo", "screenshot lo", Icons.Default.CameraAlt, true),
        QuickActionItem("claude kholo", "claude kholo", Icons.Default.Psychology, true),
        QuickActionItem("roshni kam karo", "roshni kam karo", Icons.Default.BrightnessMedium, true),
        QuickActionItem("torch jalao", "torch jalao", Icons.Default.FlashlightOn, true)
    )

    val englishActions = listOf(
        QuickActionItem("Open YouTube", "open youtube", Icons.Default.PlayArrow),
        QuickActionItem("Chapter 1 Math", "open chapter 1 mathematics", Icons.Default.Description),
        QuickActionItem("Volume 80%", "volume 80%", Icons.Default.VolumeUp),
        QuickActionItem("Take Screenshot", "take screenshot", Icons.Default.CameraAlt),
        QuickActionItem("Open Claude", "open claude", Icons.Default.Psychology),
        QuickActionItem("Brightness Up", "increase brightness", Icons.Default.BrightnessMedium)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ INSTANT COMMAND SIMULATOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = VortexCyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = VortexSurfaceElevated,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Sub-second",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 1: Hinglish & Hindi Commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hinglishActions.forEach { action ->
                FilterChip(
                    selected = false,
                    onClick = { onCommandClick(action.command) },
                    label = {
                        Text(action.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    },
                    leadingIcon = {
                        Icon(action.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VortexSurfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = VortexCyan
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("quick_chip_${action.command.replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: English Commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            englishActions.forEach { action ->
                FilterChip(
                    selected = false,
                    onClick = { onCommandClick(action.command) },
                    label = {
                        Text(action.label, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(action.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VortexSurfaceVariant.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}
