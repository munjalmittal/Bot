package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.IndexedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FileManager(private val context: Context) {

    private val tag = "BlackiFileManager"
    private var cachedFiles: List<IndexedFile> = emptyList()

    suspend fun initializeAndSeedFiles(): List<IndexedFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<IndexedFile>()

        // 1. Internal seeded study files (Chapter 1 Mathematics, Physics, etc.)
        val docsDir = File(context.filesDir, "vortex_documents")
        if (!docsDir.exists()) docsDir.mkdirs()

        val sampleFiles = listOf(
            SampleSpec("Chapter 1 Mathematics - Calculus.pdf", "Mathematics", """
                CHAPTER 1: FUNDAMENTALS OF CALCULUS & MATHEMATICAL ANALYSIS
                ===========================================================
                1.1 Limits & Continuity
                1.2 Derivatives: d/dx (x^n) = n*x^(n-1), Product & Chain Rules.
                1.3 Fundamental Theorem of Calculus: ∫_a^b f(x) dx = F(b) - F(a).
            """.trimIndent()),
            SampleSpec("Chapter 2 Physics - Mechanics.pdf", "Physics", "Newton's Laws of Motion, Kinetic & Potential Energy."),
            SampleSpec("Chapter 3 Chemistry - Organic Reactions.pdf", "Chemistry", "SN1, SN2 mechanisms and synthesis."),
            SampleSpec("Computer Science - Algorithms.txt", "Computer Science", "Sorting, Binary Search, Big-O Complexity."),
            SampleSpec("Financial Budget 2026.csv", "Finance", "Month,Category,Budget,Actual,Variance\nJan,Housing,1200,1200,0")
        )

        for (spec in sampleFiles) {
            val file = File(docsDir, spec.name)
            if (!file.exists()) file.writeText(spec.content)
            list.add(
                IndexedFile(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    uri = getUriForLocalFile(file),
                    mimeType = getMimeType(file.name),
                    sizeBytes = file.length(),
                    category = spec.category,
                    previewText = spec.content,
                    isDirectory = false
                )
            )
        }

        // 2. Comprehensive Local Storage & Folder Scanning
        try {
            scanLocalStorageFoldersAndFiles(list)
        } catch (e: Exception) {
            Log.e(tag, "Error during local storage scan: ${e.message}")
        }

        cachedFiles = list.distinctBy { it.path }
        cachedFiles
    }

    private fun scanLocalStorageFoldersAndFiles(list: MutableList<IndexedFile>) {
        val root = Environment.getExternalStorageDirectory()
        if (root == null || !root.exists() || !root.canRead()) {
            Log.w(tag, "External storage directory not readable or null")
            return
        }

        // Common top-level folders to scan thoroughly
        val targetDirs = listOf(
            File(root, "Documents"),
            File(root, "Download"),
            File(root, "Downloads"),
            File(root, "DCIM"),
            File(root, "Pictures"),
            File(root, "Books"),
            File(root, "Music"),
            root
        )

        val visitedPaths = mutableSetOf<String>()

        for (dir in targetDirs) {
            if (dir.exists() && dir.isDirectory) {
                // Index the folder itself so user can say "open folder [name]" or "Download folder kholo"
                if (!visitedPaths.contains(dir.absolutePath)) {
                    visitedPaths.add(dir.absolutePath)
                    list.add(
                        IndexedFile(
                            id = dir.absolutePath,
                            name = dir.name,
                            path = dir.absolutePath,
                            uri = getUriForLocalFile(dir),
                            mimeType = "resource/folder",
                            sizeBytes = 0,
                            category = "Local Folder",
                            previewText = "Folder: ${dir.name} (${dir.listFiles()?.size ?: 0} items)",
                            isDirectory = true
                        )
                    )
                }

                // Recursively scan files inside up to depth 3
                scanDirectoryRecursive(dir, list, visitedPaths, maxDepth = 3, currentDepth = 0)
            }
        }
    }

    private fun scanDirectoryRecursive(
        dir: File,
        list: MutableList<IndexedFile>,
        visited: MutableSet<String>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.name.startsWith(".") || file.name.equals("Android", ignoreCase = true)) {
                // Skip hidden files and Android system cache
                continue
            }

            if (file.isDirectory) {
                if (!visited.contains(file.absolutePath)) {
                    visited.add(file.absolutePath)
                    list.add(
                        IndexedFile(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            uri = getUriForLocalFile(file),
                            mimeType = "resource/folder",
                            sizeBytes = 0,
                            category = "Local Folder",
                            previewText = "Directory: ${file.name}",
                            isDirectory = true
                        )
                    )
                }
                scanDirectoryRecursive(file, list, visited, maxDepth, currentDepth + 1)
            } else if (file.isFile) {
                if (!visited.contains(file.absolutePath)) {
                    visited.add(file.absolutePath)
                    val mime = getMimeType(file.name)
                    // Index study documents, PDFs, text, spreadsheets, audio, video
                    list.add(
                        IndexedFile(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            uri = getUriForLocalFile(file),
                            mimeType = mime,
                            sizeBytes = file.length(),
                            category = getCategoryForFile(file.name),
                            previewText = if (file.length() < 50_000 && isTextReadable(file.name)) {
                                try { file.readText().take(5000) } catch (e: Exception) { null }
                            } else null,
                            isDirectory = false
                        )
                    )
                }
            }
        }
    }

    fun getCachedFiles(): List<IndexedFile> = cachedFiles

    fun findBestMatchingFileOrFolder(query: String): FileMatchResult? {
        val q = query.trim().lowercase(Locale.ROOT)
            .replace("chater", "chapter")
            .replace("maths", "mathematics")
            .replace("math", "mathematics")
            .replace("folder ", "")
            .replace("file ", "")
            .replace("doc ", "")
            .trim()

        if (q.isEmpty()) return null

        val isFolderIntent = query.contains("folder") || query.contains("फ़ोल्डर")
        val tokens = q.split(" ", "_", "-").filter { it.isNotEmpty() }

        var bestMatch: IndexedFile? = null
        var highestScore = 0.0f
        var matchReason = ""

        for (item in cachedFiles) {
            val nameLower = item.name.lowercase(Locale.ROOT)
            val pathLower = item.path.lowercase(Locale.ROOT)

            // Exact name match
            if (nameLower == q || nameLower.startsWith(q)) {
                return FileMatchResult(item, 1.0f, "Exact Match")
            }

            // Keyword token containment
            var matchedTokens = 0
            for (token in tokens) {
                if (nameLower.contains(token) || pathLower.contains(token)) {
                    matchedTokens++
                }
            }

            val tokenRatio = if (tokens.isNotEmpty()) matchedTokens.toFloat() / tokens.size.toFloat() else 0f
            val fuzzy = ActionParser.fuzzySimilarity(q, nameLower)
            var score = (tokenRatio * 0.7f) + (fuzzy * 0.3f)

            // If user explicitly asked for a folder, boost directory items
            if (isFolderIntent && item.isDirectory) {
                score += 0.25f
            }

            if (score > highestScore) {
                highestScore = score
                bestMatch = item
                matchReason = if (item.isDirectory) "Matched Local Folder" else "Matched $matchedTokens/${tokens.size} keywords"
            }
        }

        if (bestMatch != null && highestScore >= 0.3f) {
            return FileMatchResult(bestMatch, highestScore, matchReason)
        }

        return null
    }

    fun openFileOrFolder(item: IndexedFile): OpenFileOutcome {
        val targetFile = File(item.path)

        if (item.isDirectory || targetFile.isDirectory) {
            // Open Folder in System File Manager (DocumentsUI)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Fallback intent for folder viewing
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            return try {
                val pm = context.packageManager
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                    OpenFileOutcome(openedExternally = true, file = item)
                } else if (fallbackIntent.resolveActivity(pm) != null) {
                    context.startActivity(fallbackIntent)
                    OpenFileOutcome(openedExternally = true, file = item)
                } else {
                    OpenFileOutcome(
                        openedExternally = false,
                        file = item,
                        previewText = "Local Folder: ${item.path}\nContains ${targetFile.listFiles()?.size ?: 0} items."
                    )
                }
            } catch (e: Exception) {
                OpenFileOutcome(openedExternally = false, file = item, previewText = "Folder: ${item.name}", error = e.localizedMessage)
            }
        }

        // Open Document / File
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                OpenFileOutcome(openedExternally = true, file = item)
            } else {
                OpenFileOutcome(openedExternally = false, file = item, previewText = item.previewText)
            }
        } catch (e: Exception) {
            OpenFileOutcome(openedExternally = false, file = item, previewText = item.previewText, error = e.localizedMessage)
        }
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestAllFilesAccess(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity != null) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        }
    }

    fun createNewDocument(name: String, content: String, category: String = "User Notes"): IndexedFile {
        val docsDir = File(context.filesDir, "vortex_documents")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, name)
        file.writeText(content)
        val uri = getUriForLocalFile(file)
        val indexed = IndexedFile(
            id = file.absolutePath,
            name = file.name,
            path = file.absolutePath,
            uri = uri,
            mimeType = getMimeType(file.name),
            sizeBytes = file.length(),
            category = category,
            previewText = content,
            isDirectory = false
        )
        cachedFiles = (cachedFiles + indexed).distinctBy { it.path }
        return indexed
    }

    private fun getUriForLocalFile(file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    private fun getMimeType(fileName: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".doc") || lower.endsWith(".docx") -> "application/msword"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") -> "application/vnd.ms-excel"
            lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".mp4") || lower.endsWith(".mkv") -> "video/*"
            lower.endsWith(".mp3") || lower.endsWith(".wav") -> "audio/*"
            lower.endsWith(".zip") || lower.endsWith(".rar") -> "application/zip"
            else -> "*/*"
        }
    }

    private fun getCategoryForFile(fileName: String): String {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".pdf") -> "PDF Document"
            lower.endsWith(".doc") || lower.endsWith(".docx") -> "Word Document"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv") -> "Spreadsheet"
            lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "Presentation"
            lower.endsWith(".txt") -> "Text Notes"
            lower.endsWith(".png") || lower.endsWith(".jpg") -> "Image"
            lower.endsWith(".mp4") -> "Video"
            lower.endsWith(".mp3") -> "Audio"
            else -> "Document"
        }
    }

    private fun isTextReadable(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        return lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".json") || lower.endsWith(".md")
    }

    data class FileMatchResult(
        val file: IndexedFile,
        val score: Float,
        val reason: String
    )

    data class OpenFileOutcome(
        val openedExternally: Boolean,
        val file: IndexedFile,
        val previewText: String? = null,
        val error: String? = null
    )

    private data class SampleSpec(
        val name: String,
        val category: String,
        val content: String
    )
}
