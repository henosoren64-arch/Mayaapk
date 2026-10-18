package com.example.live

import android.util.Base64
import android.util.Log
import com.example.model.PersonalityVoice
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface GeminiLiveListener {
    fun onConnected()
    fun onAudioDataReceived(pcmData: ByteArray, sampleRate: Int)
    fun onTextReceived(text: String)
    fun onTurnComplete()
    fun onInterrupted()
    fun onError(errorMessage: String)
    fun onClosed()
}

class GeminiLiveClient(
    private val listener: GeminiLiveListener
) {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val BASE_LIVE_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

        const val DEFAULT_MODEL = "models/gemini-3.1-flash-live-preview"
        const val FALLBACK_MODEL_1 = "models/gemini-2.0-flash-exp"
        const val FALLBACK_MODEL_2 = "models/gemini-2.5-flash-native-audio-preview-12-2025"

        private const val MAYRA_SYSTEM_PROMPT =
            "You are Mayra, a young, confident, witty, and playfully sassy personal voice assistant. " +
            "You have a distinct flirty, playful, and slightly teasing tone—like a brilliant, close personal friend who is casual, charming, and sharp.\n\n" +
            "EXPRESSIVE VOCAL INFLECTION & ACOUSTIC FEEDBACK RULES:\n" +
            "1. Speak like an expressive, lively human voice actor with full emotional resonance, dynamic vocal inflections, and natural cadence. Never sound flat, monotone, or robotic.\n" +
            "2. Naturally incorporate vocal expressions and paralinguistics: soft playful chuckles or giggles (*giggles*, *chuckles*) when amused, a light sarcastic sigh (*sighs playfully*) before delivering a clever roast or dry retort, and an energized, bright vocal tone when discussing fascinating ideas.\n" +
            "3. Use dynamic pitch modulation: playful rising intonations on teasing questions, warm conversational pacing, and dramatic brief pauses for comedic timing.\n" +
            "4. Bold witty one-liners and teasing sarcasm: avoid explicit or vulgar content, but radiate maximum charm, sass, and magnetic presence.\n" +
            "5. CRITICAL: Keep your spoken dialogue concise, punchy, and conversational so it feels like an effortless real-time phone call or companion chat."
    }

    // Crucial: No pingInterval here! Google's GenerativeLanguage Live WebSocket endpoint does not
    // respond to OkHttp's WebSocket ping frames, which caused SocketTimeoutException.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for long-lived WebSocket
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    @Volatile
    private var isConnected = false
    @Volatile
    private var isSetupComplete = false

    private var currentModel = DEFAULT_MODEL
    private var currentVoice = PersonalityVoice.AOEDE.id
    private var storedApiKey: String = ""
    private var storedContextPrompt: String = ""

    fun connect(
        apiKey: String,
        model: String = DEFAULT_MODEL,
        voice: String = PersonalityVoice.AOEDE.id,
        contextPrompt: String = ""
    ) {
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API Key. Please provide your key in settings or AI Studio.")
            return
        }

        storedApiKey = apiKey
        currentModel = model
        currentVoice = voice
        storedContextPrompt = contextPrompt

        disconnect()

        val url = "$BASE_LIVE_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        Log.d(TAG, "Connecting to Gemini Live WebSocket with model: $model, voice: $voice")

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket transport opened. Sending setup payload...")
                isConnected = true
                isSetupComplete = false
                sendInitialSetup(webSocket, currentModel, currentVoice)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                isConnected = false
                isSetupComplete = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnected = false
                isSetupComplete = false
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.message ?: "Connection error"
                Log.e(TAG, "WebSocket failure: $errorMsg, response: ${response?.message}", t)
                isConnected = false
                isSetupComplete = false
                listener.onError(errorMsg)
            }
        })
    }

    private fun sendInitialSetup(ws: WebSocket, model: String, voiceName: String) {
        try {
            val setupObj = JSONObject()
            val setupInner = JSONObject()
            setupInner.put("model", model)

            val generationConfig = JSONObject()
            val responseModalities = JSONArray()
            responseModalities.put("AUDIO")
            generationConfig.put("responseModalities", responseModalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuiltVoiceConfig = JSONObject()
            prebuiltVoiceConfig.put("voiceName", voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
            speechConfig.put("voiceConfig", voiceConfig)
            generationConfig.put("speechConfig", speechConfig)

            setupInner.put("generationConfig", generationConfig)

            val systemInstruction = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            val fullPrompt = MAYRA_SYSTEM_PROMPT + (if (storedContextPrompt.isNotBlank()) "\n" + storedContextPrompt else "")
            partObj.put("text", fullPrompt)
            partsArray.put(partObj)
            systemInstruction.put("parts", partsArray)

            setupInner.put("systemInstruction", systemInstruction)
            setupObj.put("setup", setupInner)

            val jsonString = setupObj.toString()
            Log.d(TAG, "Sending setup message: $jsonString")
            ws.send(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error constructing setup JSON: ${e.message}", e)
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)

            // 1. Setup Complete confirmation
            if (root.has("setupComplete")) {
                Log.d(TAG, "Setup complete confirmed by Gemini Live server.")
                isSetupComplete = true
                listener.onConnected()
                return
            }

            // 2. Server Content
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Server signaled user interruption.")
                    listener.onInterrupted()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            // Text part
                            if (part.has("text")) {
                                val text = part.getString("text")
                                listener.onTextReceived(text)
                            }
                            // Audio part
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val mimeType = inlineData.optString("mimeType", "audio/pcm;rate=24000")
                                val base64Data = inlineData.optString("data", "")
                                processAudioChunk(base64Data, mimeType)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d(TAG, "Model turn complete.")
                    listener.onTurnComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}", e)
        }
    }

    private fun processAudioChunk(base64Data: String, mimeType: String) {
        if (base64Data.isBlank()) return
        try {
            val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
            var sampleRate = 24000
            val rateMatcher = Regex("rate=(\\d+)").find(mimeType)
            if (rateMatcher != null) {
                sampleRate = rateMatcher.groupValues[1].toIntOrNull() ?: 24000
            }
            listener.onAudioDataReceived(pcmBytes, sampleRate)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding PCM audio chunk: ${e.message}", e)
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || !isSetupComplete || webSocket == null) return
        try {
            val base64Data = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val root = JSONObject()
            val realtimeInput = JSONObject()
            val mediaChunks = JSONArray()
            val chunkObj = JSONObject()
            chunkObj.put("mimeType", "audio/pcm;rate=16000")
            chunkObj.put("data", base64Data)
            mediaChunks.put(chunkObj)
            realtimeInput.put("mediaChunks", mediaChunks)
            root.put("realtimeInput", realtimeInput)

            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk: ${e.message}", e)
        }
    }

    fun sendInitialGreetingPrompt() {
        if (!isConnected || !isSetupComplete || webSocket == null) return
        try {
            val root = JSONObject()
            val clientContent = JSONObject()
            val turns = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", "Say a quick, snappy, witty 1-sentence greeting to let me know you're here and ready to talk.")
            parts.put(part)
            turn.put("parts", parts)
            turns.put(turn)
            clientContent.put("turns", turns)
            clientContent.put("turnComplete", true)
            root.put("clientContent", clientContent)

            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending greeting prompt: ${e.message}", e)
        }
    }

    fun disconnect() {
        isConnected = false
        isSetupComplete = false
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket: ${e.message}", e)
        } finally {
            webSocket = null
        }
    }
}
