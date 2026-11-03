package com.example

import com.example.engine.ActionParser
import com.example.engine.BrightnessMode
import com.example.engine.ParsedIntent
import com.example.engine.VolumeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testYouTubeAliasEnglishAndHinglish() {
        val parsedEnglish = ActionParser.parse("open yt", configuredWakeWord = "Blacki")
        assertTrue(parsedEnglish.intent is ParsedIntent.OpenApp)
        assertEquals("youtube", (parsedEnglish.intent as ParsedIntent.OpenApp).targetQuery)

        val parsedHinglish = ActionParser.parse("yt kholo", configuredWakeWord = "Blacki")
        assertTrue(parsedHinglish.intent is ParsedIntent.OpenApp)
        assertEquals("youtube", (parsedHinglish.intent as ParsedIntent.OpenApp).targetQuery)

        val parsedWithWakeWord = ActionParser.parse("Blacki, open youtube for me", configuredWakeWord = "Blacki")
        assertTrue(parsedWithWakeWord.wakeWordFound)
        assertTrue(parsedWithWakeWord.intent is ParsedIntent.OpenApp)
    }

    @Test
    fun testFileSearchWithTypoAndHinglish() {
        // User's exact query: "open chater 1 mathematics"
        val parsedTypo = ActionParser.parse("open chater 1 mathematics", configuredWakeWord = "Blacki")
        assertTrue(parsedTypo.intent is ParsedIntent.OpenFile)
        val fileTarget = (parsedTypo.intent as ParsedIntent.OpenFile).targetFileNameQuery
        assertTrue(fileTarget.contains("chapter 1 mathematics"))

        // Hinglish file opening
        val parsedHinglishFile = ActionParser.parse("chapter 1 mathematics kholo", configuredWakeWord = "Blacki")
        assertTrue(parsedHinglishFile.intent is ParsedIntent.OpenFile)
    }

    @Test
    fun testVolumeControlsEnglishAndHinglish() {
        val parsedUp = ActionParser.parse("volume up", configuredWakeWord = "Blacki")
        assertTrue(parsedUp.intent is ParsedIntent.AdjustVolume)
        assertEquals(VolumeMode.UP, (parsedUp.intent as ParsedIntent.AdjustVolume).mode)

        val parsedAwazBadhao = ActionParser.parse("awaz badhao", configuredWakeWord = "Blacki")
        assertTrue(parsedAwazBadhao.intent is ParsedIntent.AdjustVolume)
        assertEquals(VolumeMode.UP, (parsedAwazBadhao.intent as ParsedIntent.AdjustVolume).mode)

        val parsedPercent = ActionParser.parse("volume 80%", configuredWakeWord = "Blacki")
        assertTrue(parsedPercent.intent is ParsedIntent.AdjustVolume)
        assertEquals(80, (parsedPercent.intent as ParsedIntent.AdjustVolume).targetPercent)
    }

    @Test
    fun testBrightnessAndScreenshot() {
        val parsedBrightness = ActionParser.parse("roshni kam karo", configuredWakeWord = "Blacki")
        assertTrue(parsedBrightness.intent is ParsedIntent.AdjustBrightness)
        assertEquals(BrightnessMode.DOWN, (parsedBrightness.intent as ParsedIntent.AdjustBrightness).mode)

        val parsedScreenshot = ActionParser.parse("screenshot lo", configuredWakeWord = "Blacki")
        assertTrue(parsedScreenshot.intent is ParsedIntent.TakeScreenshot)
    }
}
