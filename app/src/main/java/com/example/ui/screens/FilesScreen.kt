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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IndexedFile
import com.example.ui.theme.VortexAmber
import com.example.ui.theme.VortexCyan
import com.example.ui.theme.VortexDarkBg
import com.example.ui.theme.VortexGreen
import com.example.ui.theme.VortexPurple
import com.example.ui.theme.VortexSurfaceDark
import com.example.ui.theme.VortexSurfaceElevated
import com.example.ui.theme.VortexSurfaceVariant
import java.util.Locale

@Composable
fun FilesScreen(
    files: List<IndexedFile>,
    hasAllFilesAccess: Boolean,
    onOpenFile: (IndexedFile) -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRescanStorage: () -> Unit,
    onCreateNewDoc: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else {
            val q = searchQuery.lowercase(Locale.ROOT)
                .replace("chater", "chapter")
                .replace("maths", "mathematics")
            files.filter {
                it.name.lowercase(Locale.ROOT).contains(q) ||
                        it.path.lowercase(Locale.ROOT).contains(q) ||
                        it.category.lowercase(Locale.ROOT).contains(q)
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
                        "Search local folders or files (\"Download\", \"Chapter 1\")...",
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
                    .testTag("file_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Storage Permission Alert if Not Full Access
            if (!hasAllFilesAccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VortexAmber.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = VortexAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable All Files Access",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VortexAmber
                        )
                        Text(
                            text = "Grant access so Blacki can scan & open all your device folders and files.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onRequestAllFilesAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = VortexAmber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant", color = VortexDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Info header with rescan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INDEXED FILES & FOLDERS (${filteredFiles.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRescanStorage, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan Storage",
                            tint = VortexCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = VortexCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Note", fontSize = 12.sp, color = VortexCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFiles, key = { it.path }) { file ->
                    FileItemCard(
                        file = file,
                        onOpen = { onOpenFile(file) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Create Document Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newTitle = ""
                    newContent = ""
                },
                containerColor = VortexDarkBg,
                title = {
                    Text(
                        text = "Create Local Study Note",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Note Title (e.g. Chapter 4 Biology)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            label = { Text("Notes Content") },
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                onCreateNewDoc(newTitle, newContent)
                                showCreateDialog = false
                                newTitle = ""
                                newContent = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VortexCyan)
                    ) {
                        Text("Save Note", color = VortexDarkBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun FileItemCard(
    file: IndexedFile,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VortexSurfaceDark)
            .clickable { onOpen() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (file.isDirectory) VortexAmber.copy(alpha = 0.15f) else VortexSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) VortexAmber else if (file.name.contains("Math", ignoreCase = true)) VortexCyan else VortexPurple,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (file.isDirectory) VortexAmber.copy(alpha = 0.2f) else VortexSurfaceElevated,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (file.isDirectory) "Local Folder" else file.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (file.isDirectory) VortexAmber else VortexGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (file.isDirectory) file.path.substringAfterLast("0/") else "${file.mimeType.substringAfterLast("/")} • ${(file.sizeBytes / 1024).coerceAtLeast(1)} KB",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        IconButton(
            onClick = onOpen,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open item",
                tint = if (file.isDirectory) VortexAmber else VortexCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
