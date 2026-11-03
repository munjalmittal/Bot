package com.example.engine

import com.example.model.ActionType
import java.util.Locale

sealed class ParsedIntent {
    data class OpenApp(val targetQuery: String) : ParsedIntent()
    data class OpenFile(val targetFileNameQuery: String) : ParsedIntent()
    data class AdjustVolume(val mode: VolumeMode, val targetPercent: Int? = null) : ParsedIntent()
    data class AdjustBrightness(val mode: BrightnessMode, val targetPercent: Int? = null) : ParsedIntent()
    object TakeScreenshot : ParsedIntent()
    data class ToggleFlashlight(val enable: Boolean?) : ParsedIntent()
    data class OpenSettings(val screenType: String) : ParsedIntent()
    data class WebSearch(val query: String) : ParsedIntent()
    object Unknown : ParsedIntent()
}

enum class VolumeMode {
    UP, DOWN, MUTE, UNMUTE, MAX, SET_PERCENT
}

enum class BrightnessMode {
    UP, DOWN, MAX, MIN, SET_PERCENT
}

data class ParsedCommand(
    val rawText: String,
    val cleanedCommand: String,
    val wakeWordFound: Boolean,
    val intent: ParsedIntent,
    val detectedLanguage: String = "English / Hinglish",
    val confidence: Float = 1.0f
)

object ActionParser {

    private val DEFAULT_WAKE_WORDS = listOf(
        "blacki", "blackie", "blacky", "ब्लैकी", "ब्लेकी",
        "hey blacki", "oye blacki", "arrey blacki", "blacki suno", "blacki ji",
        "vortex", "nova", "jarvis", "aura"
    )

    // Default alias mapping for instant match across English, Hindi transliteration, and shortcuts
    val BUILT_IN_APP_ALIASES = mapOf(
        "yt" to "youtube",
        "यूट्यूब" to "youtube",
        "fb" to "facebook",
        "फेसबुक" to "facebook",
        "ig" to "instagram",
        "insta" to "instagram",
        "इन्स्टाग्राम" to "instagram",
        "wa" to "whatsapp",
        "watsapp" to "whatsapp",
        "whatapp" to "whatsapp",
        "व्हाट्सएप" to "whatsapp",
        "calc" to "calculator",
        "कैलकुलेटर" to "calculator",
        "hisab" to "calculator",
        "cam" to "camera",
        "कैमरा" to "camera",
        "msg" to "messages",
        "messages" to "messages",
        "sms" to "messages",
        "sandesh" to "messages",
        "gm" to "gmail",
        "mail" to "gmail",
        "email" to "gmail",
        "gmaps" to "maps",
        "map" to "maps",
        "maps" to "google maps",
        "nakshe" to "maps",
        "browser" to "chrome",
        "chrome" to "chrome",
        "web" to "chrome",
        "claude" to "claude",
        "क्लाउड" to "claude",
        "gpt" to "chatgpt",
        "chatgpt" to "chatgpt",
        "spotify" to "spotify",
        "music" to "spotify",
        "gaana" to "spotify",
        "phone" to "phone",
        "dialer" to "phone",
        "call" to "phone",
        "gallery" to "photos",
        "photos" to "photos",
        "tasveere" to "photos",
        "store" to "play store",
        "playstore" to "play store",
        "files" to "files",
        "docs" to "docs",
        "sheets" to "sheets",
        "notes" to "notes"
    )

    fun parse(
        input: String,
        configuredWakeWord: String = "Blacki",
        requireWakeWord: Boolean = false
    ): ParsedCommand {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) {
            return ParsedCommand(input, "", false, ParsedIntent.Unknown)
        }

        val allWakeWords = (DEFAULT_WAKE_WORDS + listOf(
            configuredWakeWord.trim().lowercase(Locale.ROOT),
            "hey ${configuredWakeWord.trim().lowercase(Locale.ROOT)}",
            "oye ${configuredWakeWord.trim().lowercase(Locale.ROOT)}"
        )).distinct()

        var cleaned = trimmed
        var wakeWordFound = false

        // 1. Detect and strip wake word
        for (wake in allWakeWords) {
            val prefixes = listOf(
                "hey $wake", "oye $wake", "arrey $wake", "ok $wake", "hi $wake",
                "$wake suno", "$wake ji", "$wake,", "$wake ", wake
            )
            for (p in prefixes) {
                if (cleaned.startsWith(p)) {
                    wakeWordFound = true
                    cleaned = cleaned.removePrefix(p).trim()
                    break
                }
            }
            if (wakeWordFound) break
        }

        if (!wakeWordFound && cleaned.contains(configuredWakeWord.lowercase(Locale.ROOT))) {
            wakeWordFound = true
            cleaned = cleaned.replace(configuredWakeWord.lowercase(Locale.ROOT), "").trim()
        }

        // 2. Strip common English & Hinglish conversational filler prefixes & suffixes
        // Hinglish: "zara", "bhai", "yaar", "kripya", "mera", "meri", "ek baar", "jaldi", "please"
        cleaned = cleaned
            .replaceFirst(Regex("^(please|can you|could you|would you|kindly|i command you to|command|zara|bhai|yaar|kripya|jaldi|arre|arrey)\\s+"), "")
            .replace(Regex("\\s+(for me|please|yaar|bhai|zara|jaldi|na)$"), "")
            .trim()

        val isHindiScript = cleaned.any { it in '\u0900'..'\u097F' }
        val detectedLang = if (isHindiScript) "Hindi (हिंदी)" else if (containsHinglishMarkers(cleaned)) "Hinglish" else "English"

        if (requireWakeWord && !wakeWordFound) {
            return ParsedCommand(input, cleaned, false, ParsedIntent.Unknown, detectedLang)
        }

        val intent = determineIntent(cleaned)
        return ParsedCommand(input, cleaned, wakeWordFound, intent, detectedLang)
    }

    private fun containsHinglishMarkers(s: String): Boolean {
        val markers = listOf("kholo", "chalao", "badhao", "kam karo", "chalu", "band karo", "awaz", "roshni", "gaana", "jalao", "bujhao", "dikhao", "wali")
        return markers.any { s.contains(it) }
    }

    private fun determineIntent(cmd: String): ParsedIntent {
        if (cmd.isEmpty()) return ParsedIntent.Unknown

        // 1. Screenshot (English / Hinglish / Hindi)
        // "screenshot lo", "photo lo screen ki", "screen capture karo", "screenshot kheecho", "स्क्रीनशॉट लो"
        if (cmd.contains("screenshot") || cmd.contains("screen capture") || cmd.contains("snap screen") ||
            cmd.contains("स्क्रीनशॉट") || cmd.contains("photo lo screen") || cmd.contains("screen ki photo")
        ) {
            return ParsedIntent.TakeScreenshot
        }

        // 2. Volume / Awaz (English / Hinglish / Hindi)
        val volumeIntent = checkVolumeIntent(cmd)
        if (volumeIntent != null) return volumeIntent

        // 3. Brightness / Roshni / Light (English / Hinglish / Hindi)
        val brightnessIntent = checkBrightnessIntent(cmd)
        if (brightnessIntent != null) return brightnessIntent

        // 4. Flashlight / Torch / Batti (English / Hinglish / Hindi)
        val torchIntent = checkTorchIntent(cmd)
        if (torchIntent != null) return torchIntent

        // 5. System Settings (Wifi, Bluetooth, Settings)
        if (cmd.contains("wifi") || cmd.contains("wi-fi") || cmd.contains("वाईफाई")) {
            return ParsedIntent.OpenSettings("wifi")
        }
        if (cmd.contains("bluetooth") || cmd.contains("ब्लूटूथ")) {
            return ParsedIntent.OpenSettings("bluetooth")
        }
        if (cmd == "open settings" || cmd == "settings" || cmd.contains("settings kholo") || cmd.contains("सेटिंग्स खोलो")) {
            return ParsedIntent.OpenSettings("main")
        }

        // 6. Explicit File Opening ("open chapter 1 mathematics", "chapter 1 mathematics kholo", "chater 1 mathematics")
        val fileIntent = checkFileIntent(cmd)
        if (fileIntent != null) return fileIntent

        // 7. App Opening ("open yt", "youtube kholo", "yt chalao", "claude kholo", "open claude")
        val appIntent = checkAppIntent(cmd)
        if (appIntent != null) return appIntent

        // 8. Direct alias lookup ("yt", "claude", "whatsapp", "calc")
        val directAlias = BUILT_IN_APP_ALIASES[cmd]
        if (directAlias != null) {
            return ParsedIntent.OpenApp(directAlias)
        }

        // 9. Web search fallback ("search ...", "google ...", "dhundo ...")
        if (cmd.startsWith("search ") || cmd.startsWith("google ") || cmd.startsWith("dhundo ")) {
            val query = cmd.substringAfter(" ").trim()
            return ParsedIntent.WebSearch(query)
        }

        // Default: If short command (1-3 words), try app opening
        if (cmd.split(" ").size <= 3) {
            return ParsedIntent.OpenApp(cmd)
        }

        return ParsedIntent.Unknown
    }

    private fun checkVolumeIntent(cmd: String): ParsedIntent? {
        val hasVolumeKeyword = cmd.contains("volume") || cmd.contains("sound") || cmd.contains("audio") ||
                cmd.contains("awaz") || cmd.contains("awaaz") || cmd.contains("aawaz") || cmd.contains("आवाज़") || cmd.contains("वॉल्यूम")

        // Mute: "mute", "chup", "shant", "awaz band", "silent karo", "आवाज़ बंद"
        if (cmd.contains("mute") || cmd.contains("silence") || cmd.contains("silent") ||
            cmd.contains("chup") || cmd.contains("awaz band") || cmd.contains("awaaz band") || cmd.contains("shant") ||
            cmd.contains("आवाज़ बंद") || cmd.contains("म्यूट")
        ) {
            return ParsedIntent.AdjustVolume(VolumeMode.MUTE)
        }

        // Unmute
        if (cmd.contains("unmute") || cmd.contains("awaz chalu karo") || cmd.contains("awaz kholo")) {
            return ParsedIntent.AdjustVolume(VolumeMode.UNMUTE)
        }

        // Max volume: "max volume", "full volume", "poori awaz", "sound full", "loudest"
        if (cmd.contains("max volume") || cmd.contains("full volume") || cmd.contains("loudest") ||
            cmd.contains("poori awaz") || cmd.contains("full awaz") || cmd.contains("sound full") ||
            cmd.contains("पूरी आवाज़") || cmd.contains("फुल वॉल्यूम")
        ) {
            return ParsedIntent.AdjustVolume(VolumeMode.MAX)
        }

        // Specific percent: "volume 80%", "awaz 50 karo", "volume 70"
        val percentMatch = Regex("(\\d{1,3})\\s*(%|percent)?").find(cmd)
        if (percentMatch != null && hasVolumeKeyword) {
            val num = percentMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 0..100) {
                return ParsedIntent.AdjustVolume(VolumeMode.SET_PERCENT, num)
            }
        }

        // Up: "increase volume", "volume up", "awaz badhao", "awaz tez karo", "tez karo", "sound badhao", "आवाज़ बढ़ाओ", "तेज़ करो"
        if (cmd.contains("increase") || cmd.contains("volume up") || cmd.contains("louder") || cmd.contains("raise") ||
            cmd.contains("badhao") || cmd.contains("badao") || cmd.contains("tez karo") || cmd.contains("unchi karo") ||
            cmd.contains("बढ़ाओ") || cmd.contains("तेज़ करो")
        ) {
            if (hasVolumeKeyword || cmd == "louder" || cmd.contains("tez karo") || cmd.contains("awaz badhao")) {
                return ParsedIntent.AdjustVolume(VolumeMode.UP)
            }
        }

        // Down: "decrease volume", "volume down", "awaz kam karo", "dheemi karo", "sound kam", "आवाज़ कम करो", "धीमी करो"
        if (cmd.contains("decrease") || cmd.contains("volume down") || cmd.contains("lower") || cmd.contains("softer") ||
            cmd.contains("kam karo") || cmd.contains("dheemi karo") || cmd.contains("dheere karo") || cmd.contains("ghatao") ||
            cmd.contains("कम करो") || cmd.contains("धीमी करो")
        ) {
            if (hasVolumeKeyword || cmd == "softer" || cmd.contains("dheemi karo") || cmd.contains("awaz kam karo")) {
                return ParsedIntent.AdjustVolume(VolumeMode.DOWN)
            }
        }

        return null
    }

    private fun checkBrightnessIntent(cmd: String): ParsedIntent? {
        val hasBrightnessKeyword = cmd.contains("brightness") || cmd.contains("roshni") || cmd.contains("screen light") ||
                cmd.contains("light") || cmd.contains("display light") || cmd.contains("ब्राइटनेस") || cmd.contains("रोशनी")

        if (cmd.contains("max brightness") || cmd.contains("full brightness") || cmd.contains("poori roshni") || cmd.contains("full light")) {
            return ParsedIntent.AdjustBrightness(BrightnessMode.MAX)
        }
        if (cmd.contains("min brightness") || cmd.contains("lowest brightness") || cmd.contains("kam se kam light")) {
            return ParsedIntent.AdjustBrightness(BrightnessMode.MIN)
        }

        val percentMatch = Regex("(\\d{1,3})\\s*(%|percent)?").find(cmd)
        if (percentMatch != null && hasBrightnessKeyword) {
            val num = percentMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 0..100) {
                return ParsedIntent.AdjustBrightness(BrightnessMode.SET_PERCENT, num)
            }
        }

        // Brightness Up: "increase brightness", "brightness up", "brighter", "roshni badhao", "light badhao", "रोशनी बढ़ाओ"
        if (cmd.contains("increase brightness") || cmd.contains("brightness up") || cmd.contains("brighter") ||
            cmd.contains("roshni badhao") || cmd.contains("light badhao") || cmd.contains("रोशनी बढ़ाओ") || cmd.contains("ब्राइटनेस बढ़ाओ")
        ) {
            return ParsedIntent.AdjustBrightness(BrightnessMode.UP)
        }

        // Brightness Down: "decrease brightness", "brightness down", "dim screen", "roshni kam karo", "light kam karo", "रोशनी कम करो"
        if (cmd.contains("decrease brightness") || cmd.contains("brightness down") || cmd.contains("dim screen") ||
            cmd.contains("lower brightness") || cmd.contains("roshni kam karo") || cmd.contains("light kam karo") ||
            cmd.contains("रोशनी कम करो") || cmd.contains("ब्राइटनेस कम करो")
        ) {
            return ParsedIntent.AdjustBrightness(BrightnessMode.DOWN)
        }

        return null
    }

    private fun checkTorchIntent(cmd: String): ParsedIntent? {
        val isTorchRelated = cmd.contains("flashlight") || cmd.contains("torch") || cmd.contains("batti") ||
                cmd.contains("फ्लैशलाइट") || cmd.contains("टॉर्च")
        if (!isTorchRelated) return null

        val enable = when {
            cmd.contains("off") || cmd.contains("band") || cmd.contains("bujhao") || cmd.contains("stop") ||
                    cmd.contains("बंद") || cmd.contains("बुझाओ") -> false
            cmd.contains("on") || cmd.contains("chalao") || cmd.contains("chalu") || cmd.contains("jalao") ||
                    cmd.contains("चालू") || cmd.contains("जलाओ") -> true
            else -> null // toggle
        }
        return ParsedIntent.ToggleFlashlight(enable)
    }

    private fun checkFileIntent(cmd: String): ParsedIntent? {
        // Handle variations:
        // "open chapter 1 mathematics"
        // "chapter 1 mathematics kholo"
        // "chater 1 mathematics" (user's exact typo!)
        // "maths ka chapter 1 kholo"
        // "chapter 1 mathematics wali file kholo"
        // "चैप्टर 1 मैथमेटिक्स खोलो"

        val normalized = cmd.replace("chater", "chapter")
            .replace("maths", "mathematics")
            .replace("math", "mathematics")
            .replace("मैथमेटिक्स", "mathematics")
            .replace("चैप्टर", "chapter")

        val hasFileKeywords = normalized.contains("chapter") || normalized.contains("file") ||
                normalized.contains("notes") || normalized.contains(".pdf") || normalized.contains(".doc") ||
                normalized.contains(".csv") || normalized.contains(".txt") || normalized.contains("document") ||
                normalized.contains("kitab") || normalized.contains("book") || normalized.contains("folder") ||
                normalized.contains("download") || normalized.contains("documents") || normalized.contains("dcim") ||
                normalized.contains("फ़ोल्डर") || normalized.contains("डॉक्यूमेंट")

        if (!hasFileKeywords) return null

        // Strip verbs in English and Hinglish: "open", "kholo", "chalao", "dikhao", "wali file", "wali", "ka", "ki"
        var query = normalized
            .replace(Regex("^(open folder|open file|open doc|open document|open directory|open|view file|find file|view|read|kholo|dikhao)\\s+"), "")
            .replace(Regex("\\s+(kholo|chalao|dikhao|wali file|wali doc|wali|lao|file|doc|folder|directory|open|kar do)$"), "")
            .replace("wali file", "")
            .replace("folder", "")
            .replace("file", "")
            .trim()

        if (query.isNotEmpty()) {
            return ParsedIntent.OpenFile(query)
        }

        return null
    }

    private fun checkAppIntent(cmd: String): ParsedIntent? {
        // Matches English: "open yt", "launch youtube", "go to claude"
        // Matches Hinglish prefix & suffix:
        // "youtube kholo", "yt chalao", "claude chalu karo", "kholo youtube", "chalao yt"
        // "यूट्यूब खोलो", "व्हाट्सएप चालू करो"

        var target = cmd

        // Hinglish suffix removal: "youtube kholo" -> "youtube", "yt chalao" -> "yt"
        val suffixRegex = Regex("\\s+(kholo|chalao|chalu karo|start karo|shuru karo|open karo|laga do|chala do|dikhao|kholey|खोलो|चलाओ|शुरू करो)$")
        if (target.contains(suffixRegex)) {
            target = target.replace(suffixRegex, "").trim()
        }

        // Prefix removal: "open youtube", "launch yt", "kholo claude", "chalao yt"
        val prefixRegex = Regex("^(open app|open|launch|start|run|go to|kholo|chalao|chalu karo|shuru karo|खोलो|चलाओ)\\s+")
        if (target.contains(prefixRegex)) {
            target = target.replace(prefixRegex, "").trim()
        }

        // Clean extra particles like "ko", "app ko"
        target = target.replace(Regex("\\s+(app|application|ko)$"), "").trim()

        val aliased = BUILT_IN_APP_ALIASES[target] ?: target
        if (aliased.isNotEmpty()) {
            return ParsedIntent.OpenApp(aliased)
        }

        return null
    }

    fun fuzzySimilarity(s1: String, s2: String): Float {
        val str1 = s1.lowercase(Locale.ROOT)
        val str2 = s2.lowercase(Locale.ROOT)
        if (str1 == str2) return 1.0f
        if (str1.contains(str2) || str2.contains(str1)) return 0.9f

        val len1 = str1.length
        val len2 = str2.length
        val maxLen = maxOf(len1, len2)
        if (maxLen == 0) return 1.0f

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        val distance = dp[len1][len2]
        return 1.0f - (distance.toFloat() / maxLen)
    }
}
