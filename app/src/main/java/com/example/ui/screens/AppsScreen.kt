package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InstalledApp
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexSurfaceDark
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant
import java.util.Locale

@Composable
fun AppsScreen(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onAddAlias: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAppForAlias by remember { mutableStateOf<InstalledApp?>(null) }
    var newAliasInput by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else {
            val q = searchQuery.lowercase(Locale.ROOT)
            apps.filter {
                it.label.lowercase(Locale.ROOT).contains(q) ||
                        it.packageName.lowercase(Locale.ROOT).contains(q) ||
                        it.aliases.any { a -> a.lowercase(Locale.ROOT).contains(q) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search apps or voice aliases (\"yt\", \"calc\")...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = VortexCyan
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VortexCyan,
                    unfocusedBorderColor = VortexSurfaceElevated,
                    focusedContainerColor = VortexSurfaceDark,
                    unfocusedContainerColor = VortexSurfaceDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App count and info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED APPS (${filteredApps.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Say \"[Alias] kholo\"",
                    fontSize = 11.sp,
                    color = VortexCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        onLaunch = { onLaunchApp(app) },
                        onAddAliasClick = {
                            selectedAppForAlias = app
                            showAddDialog = true
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Alias Dialog
        if (showAddDialog && selectedAppForAlias != null) {
            val app = selectedAppForAlias!!
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    newAliasInput = ""
                },
                containerColor = VortexDarkBg,
                title = {
                    Text(
                        text = "Add Voice Alias for ${app.label}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Assign a shortcut nickname (e.g. \"yt\", \"claude\", \"my notes\") to launch this app instantly with voice.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newAliasInput,
                            onValueChange = { newAliasInput = it },
                            placeholder = { Text("Enter alias (e.g. 'yt', 'fav')") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_alias_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAliasInput.isNotBlank()) {
                                onAddAlias(newAliasInput, app.packageName)
                                showAddDialog = false
                                newAliasInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VortexCyan),
                        modifier = Modifier.testTag("save_alias_button")
                    ) {
                        Text("Save Alias", color = VortexDarkBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun AppItemCard(
    app: InstalledApp,
    onLaunch: () -> Unit,
    onAddAliasClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VortexSurfaceDark)
            .clickable { onLaunch() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VortexSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = VortexCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = app.packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )

            if (app.aliases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    app.aliases.take(3).forEach { alias ->
                        Surface(
                            color = VortexSurfaceElevated,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "alias: $alias",
                                fontSize = 10.sp,
                                color = VortexCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onAddAliasClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alias",
                tint = VortexPurple,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onLaunch,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Launch,
                contentDescription = "Launch App",
                tint = VortexCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
