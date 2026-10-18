package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioCueGenerator
import com.example.data.model.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mayra", appName)
    }

    @Test
    fun testAudioCuesGeneration() {
        val wakeChime = AudioCueGenerator.generateWakeChime()
        assertTrue(wakeChime.isNotEmpty())

        val popCue = AudioCueGenerator.generateInterruptionPop()
        assertTrue(popCue.isNotEmpty())

        val sleepChime = AudioCueGenerator.generateSleepChime()
        assertTrue(sleepChime.isNotEmpty())
    }

    @Test
    fun testUserProfileDefaults() {
        val profile = UserProfileEntity(profileName = "Bestie")
        assertEquals("Darling", profile.userNickname)
        assertEquals("Playful & Sassy", profile.sassLevel)
    }
}
