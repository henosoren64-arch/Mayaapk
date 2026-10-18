package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioRecordManager(
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onInterruptionDetected: () -> Unit
) {
    companion object {
        private const val TAG = "AudioRecordManager"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_BYTES = 2048 // 1024 samples = 64ms at 16kHz
        private const val SPEECH_INTERRUPT_THRESHOLD = 0.12f // Normalized RMS threshold for interruption
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isModelCurrentlySpeaking: Boolean = false

    private var speechFramesCount = 0

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (recordingJob?.isActive == true) return true

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_BYTES * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC source if VOICE_COMMUNICATION is not available
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.")
                return false
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord: ${e.message}", e)
            return false
        }

        recordingJob = scope.launch {
            val audioBuffer = ByteArray(CHUNK_SIZE_BYTES)
            val shortBuffer = ShortArray(CHUNK_SIZE_BYTES / 2)

            while (isActive) {
                val record = audioRecord ?: break
                val bytesRead = record.read(audioBuffer, 0, CHUNK_SIZE_BYTES)

                if (bytesRead > 0) {
                    if (isMuted) {
                        onAmplitudeChanged(0f)
                        continue
                    }

                    // Convert to shorts for RMS amplitude calculation
                    ByteBuffer.wrap(audioBuffer, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(shortBuffer, 0, bytesRead / 2)

                    var sumOfSquares = 0.0
                    for (i in 0 until (bytesRead / 2)) {
                        val sample = shortBuffer[i].toDouble()
                        sumOfSquares += sample * sample
                    }

                    val meanSquare = sumOfSquares / (bytesRead / 2)
                    val rms = sqrt(meanSquare)
                    // Normalize RMS to 0.0 - 1.0 (typical speaking range 0 - 6000)
                    val normalizedAmplitude = (rms / 6000.0).toFloat().coerceIn(0f, 1f)
                    onAmplitudeChanged(normalizedAmplitude)

                    // Check for user interruption while Mayra is speaking
                    if (isModelCurrentlySpeaking && normalizedAmplitude > SPEECH_INTERRUPT_THRESHOLD) {
                        speechFramesCount++
                        if (speechFramesCount >= 2) {
                            Log.d(TAG, "User interruption detected (amplitude: $normalizedAmplitude)")
                            onInterruptionDetected()
                            speechFramesCount = 0
                        }
                    } else {
                        speechFramesCount = 0
                    }

                    // Send audio chunk to Gemini Live
                    val chunkToSend = audioBuffer.copyOf(bytesRead)
                    onAudioChunk(chunkToSend)
                }
            }
        }

        Log.d(TAG, "AudioRecord started successfully.")
        return true
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}", e)
        } finally {
            audioRecord = null
            onAmplitudeChanged(0f)
        }
    }
}
