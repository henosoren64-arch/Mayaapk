package com.example.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioCueGenerator {
    private const val SAMPLE_RATE = 24000

    /**
     * Shimmering melodic rising chime for Mayra's wake-up/connection.
     */
    fun generateWakeChime(): ByteArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 987.77, 1046.50) // C5, E5, G5, B5, C6
        val noteDuration = 0.08 // 80ms per note
        val tailDuration = 0.25 // reverb decay
        val totalDuration = (notes.size * noteDuration) + tailDuration
        val totalSamples = (totalDuration * SAMPLE_RATE).toInt()

        val buffer = ShortArray(totalSamples)

        for (i in notes.indices) {
            val freq = notes[i]
            val startSample = (i * noteDuration * SAMPLE_RATE).toInt()
            val noteSamples = ((noteDuration + tailDuration) * SAMPLE_RATE).toInt()

            for (s in 0 until noteSamples) {
                val idx = startSample + s
                if (idx >= totalSamples) break
                val t = s.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 9.0) // exponential decay
                // Fundamental + soft harmonic shimmer
                val sampleVal = (sin(2 * PI * freq * t) * 0.7 + sin(2 * PI * freq * 2 * t) * 0.3) * envelope
                val current = buffer[idx] + (sampleVal * 7000).toInt()
                buffer[idx] = current.coerceIn(-32767, 32767).toShort()
            }
        }

        return shortsToBytes(buffer)
    }

    /**
     * Playful, soft cute pop/vinyl click when user interrupts Mayra.
     */
    fun generateInterruptionPop(): ByteArray {
        val duration = 0.06 // 60ms
        val totalSamples = (duration * SAMPLE_RATE).toInt()
        val buffer = ShortArray(totalSamples)

        for (s in 0 until totalSamples) {
            val t = s.toDouble() / SAMPLE_RATE
            val freq = 450.0 * exp(-t * 40.0) // pitch drop
            val envelope = exp(-t * 60.0)
            val sampleVal = sin(2 * PI * freq * t) * envelope
            buffer[s] = (sampleVal * 12000).toInt().toShort()
        }

        return shortsToBytes(buffer)
    }

    /**
     * Gentle descending chime when disconnecting.
     */
    fun generateSleepChime(): ByteArray {
        val notes = doubleArrayOf(880.0, 783.99, 659.25, 523.25) // A5, G5, E5, C5
        val noteDuration = 0.09
        val tailDuration = 0.3
        val totalDuration = (notes.size * noteDuration) + tailDuration
        val totalSamples = (totalDuration * SAMPLE_RATE).toInt()

        val buffer = ShortArray(totalSamples)

        for (i in notes.indices) {
            val freq = notes[i]
            val startSample = (i * noteDuration * SAMPLE_RATE).toInt()
            val noteSamples = ((noteDuration + tailDuration) * SAMPLE_RATE).toInt()

            for (s in 0 until noteSamples) {
                val idx = startSample + s
                if (idx >= totalSamples) break
                val t = s.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 7.0)
                val sampleVal = sin(2 * PI * freq * t) * envelope
                val current = buffer[idx] + (sampleVal * 6000).toInt()
                buffer[idx] = current.coerceIn(-32767, 32767).toShort()
            }
        }

        return shortsToBytes(buffer)
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in shorts) {
            byteBuffer.putShort(s)
        }
        return byteBuffer.array()
    }
}
