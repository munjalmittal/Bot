package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import com.example.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AppManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vortex_app_aliases", Context.MODE_PRIVATE)

    private var cachedApps: List<InstalledApp> = emptyList()

    // Well-known web URLs for popular services when native app is not installed
    private val webFallbacks = mapOf(
        "youtube" to "https://www.youtube.com",
        "yt" to "https://www.youtube.com",
        "claude" to "https://claude.ai",
        "chatgpt" to "https://chatgpt.com",
        "instagram" to "https://www.instagram.com",
        "facebook" to "https://www.facebook.com",
        "twitter" to "https://x.com",
        "x" to "https://x.com",
        "reddit" to "https://www.reddit.com",
        "spotify" to "https://open.spotify.com",
        "github" to "https://github.com",
        "gmail" to "https://mail.google.com"
    )

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val userAliases = getAllCustomAliases()

        val list = resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)

            // Gather aliases for this app
            val matchedAliases = mutableListOf<String>()
            ActionParser.BUILT_IN_APP_ALIASES.forEach { (alias, target) ->
                if (label.lowercase(Locale.ROOT).contains(target) || pkg.lowercase(Locale.ROOT).contains(target)) {
                    matchedAliases.add(alias)
                }
            }
            // Add custom user aliases
            userAliases.filter { it.value == pkg }.forEach { (alias, _) ->
                if (!matchedAliases.contains(alias)) {
                    matchedAliases.add(alias)
                }
            }

            InstalledApp(
                packageName = pkg,
                label = label,
                icon = icon,
                aliases = matchedAliases.distinct()
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }

        cachedApps = list
        list
    }

    fun getCachedApps(): List<InstalledApp> = cachedApps

    fun findApp(query: String): MatchResult? {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return null

        // 1. Check custom user alias first
        val customPkg = prefs.getString("alias_$q", null)
        if (customPkg != null) {
            val app = cachedApps.find { it.packageName == customPkg }
            if (app != null) return MatchResult(app, 1.0f, "Custom Alias '$q'")
        }

        // 2. Check built-in aliases
        val targetName = ActionParser.BUILT_IN_APP_ALIASES[q] ?: q

        // 3. Exact label or package match
        val exact = cachedApps.find {
            it.label.lowercase(Locale.ROOT) == targetName ||
                    it.packageName.lowercase(Locale.ROOT) == targetName ||
                    it.label.lowercase(Locale.ROOT) == q
        }
        if (exact != null) return MatchResult(exact, 1.0f, "Exact Match")

        // 4. Starts with or contains match
        val containsMatch = cachedApps.find {
            it.label.lowercase(Locale.ROOT).contains(targetName) ||
                    targetName.contains(it.label.lowercase(Locale.ROOT)) ||
                    it.aliases.contains(q) ||
                    it.aliases.contains(targetName)
        }
        if (containsMatch != null) return MatchResult(containsMatch, 0.9f, "Alias/Substring Match")

        // 5. Fuzzy match
        var bestApp: InstalledApp? = null
        var bestScore = 0.0f
        for (app in cachedApps) {
            val labelScore = ActionParser.fuzzySimilarity(q, app.label)
            val targetScore = ActionParser.fuzzySimilarity(targetName, app.label)
            val score = maxOf(labelScore, targetScore)
            if (score > bestScore) {
                bestScore = score
                bestApp = app
            }
        }

        if (bestApp != null && bestScore >= 0.55f) {
            return MatchResult(bestApp, bestScore, "Fuzzy Match (${(bestScore * 100).toInt()}%)")
        }

        return null
    }

    fun launchApp(app: InstalledApp): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun launchWebFallback(query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        val url = webFallbacks[q] ?: "https://www.google.com/search?q=${Uri.encode(query)}"
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addCustomAlias(alias: String, packageName: String) {
        prefs.edit().putString("alias_${alias.trim().lowercase(Locale.ROOT)}", packageName).apply()
    }

    fun removeCustomAlias(alias: String) {
        prefs.edit().remove("alias_${alias.trim().lowercase(Locale.ROOT)}").apply()
    }

    fun getAllCustomAliases(): Map<String, String> {
        val all = prefs.all
        val map = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (key.startsWith("alias_") && value is String) {
                map[key.removePrefix("alias_")] = value
            }
        }
        return map
    }

    data class MatchResult(
        val app: InstalledApp,
        val confidence: Float,
        val matchType: String
    )
}
