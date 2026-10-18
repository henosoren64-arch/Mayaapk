package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class AudioTrackPlayer(
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackEnded: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) {
    companion object {
        private const val TAG = "AudioTrackPlayer"
        const val DEFAULT_SAMPLE_RATE = 24000
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isPlaying: Boolean = false
        private set

    private var currentSampleRate = DEFAULT_SAMPLE_RATE

    fun initialize(sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (audioTrack != null && currentSampleRate == sampleRate) return
        release()
        currentSampleRate = sampleRate

        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 8192)

        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()
            startPlaybackWorker()
            Log.d(TAG, "AudioTrack initialized with sample rate: $sampleRate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}", e)
        }
    }

    private fun startPlaybackWorker() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val shortBuffer = ShortArray(4096)

            while (isActive) {
                val chunk = try {
                    audioQueue.poll(100, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    null
                }

                if (chunk != null && chunk.isNotEmpty()) {
                    if (!isPlaying) {
                        isPlaying = true
                        onPlaybackStarted()
                    }

                    // Compute RMS for visualization
                    val numSamples = chunk.size / 2
                    if (numSamples > 0) {
                        val neededSize = minOf(numSamples, shortBuffer.size)
                        ByteBuffer.wrap(chunk, 0, neededSize * 2)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer()
                            .get(shortBuffer, 0, neededSize)

                        var sumOfSquares = 0.0
                        for (i in 0 until neededSize) {
                            val sample = shortBuffer[i].toDouble()
                            sumOfSquares += sample * sample
                        }
                        val rms = sqrt(sumOfSquares / neededSize)
                        val amplitude = (rms / 6000.0).toFloat().coerceIn(0f, 1f)
                        onAmplitudeChanged(amplitude)
                    }

                    // Write to track
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (isPlaying && audioQueue.isEmpty()) {
                        isPlaying = false
                        onAmplitudeChanged(0f)
                        onPlaybackEnded()
                    }
                }
            }
        }
    }

    fun enqueueAudio(data: ByteArray, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (audioTrack == null || currentSampleRate != sampleRate) {
            initialize(sampleRate)
        }
        audioQueue.offer(data)
    }

    /**
     * Instantly flushes and silences the playback for interruption handling.
     */
    fun interrupt() {
        audioQueue.clear()
        try {
            audioTrack?.let {
                it.pause()
                it.flush()
                it.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting AudioTrack: ${e.message}", e)
        }
        if (isPlaying) {
            isPlaying = false
            onAmplitudeChanged(0f)
            onPlaybackEnded()
        }
        Log.d(TAG, "AudioTrack playback interrupted and flushed.")
    }

    fun release() {
        playbackJob?.cancel()
        playbackJob = null
        audioQueue.clear()
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack: ${e.message}", e)
        } finally {
            audioTrack = null
            isPlaying = false
            onAmplitudeChanged(0f)
        }
    }
}
