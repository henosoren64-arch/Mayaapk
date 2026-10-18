package com.example.session

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.audio.AudioRecordManager
import com.example.audio.AudioTrackPlayer
import com.example.data.MayraDatabase
import com.example.data.model.MemoryEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.MayraMemoryRepository
import com.example.live.GeminiLiveClient
import com.example.live.GeminiLiveListener
import com.example.model.AssistantStatus
import com.example.model.ConnectionState
import com.example.model.MayraSass
import com.example.model.MayraUiState
import com.example.model.PersonalityVoice
import com.example.service.MayraVoiceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object MayraSessionManager : GeminiLiveListener {
    private const val TAG = "MayraSessionManager"
    private const val PREFS_NAME = "mayra_prefs"
    private const val PREF_KEY_API_KEY = "custom_gemini_api_key"
    private const val PREF_KEY_VOICE = "selected_personality_voice"
    private const val PREF_KEY_MODEL = "selected_gemini_model"
    private const val PREF_KEY_BG = "background_listening_enabled"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(MayraUiState())
    val uiState: StateFlow<MayraUiState> = _uiState.asStateFlow()

    private var sharedPrefs: SharedPreferences? = null
    private var repository: MayraMemoryRepository? = null

    private lateinit var liveClient: GeminiLiveClient
    private lateinit var recordManager: AudioRecordManager
    private lateinit var trackPlayer: AudioTrackPlayer

    private var isInitialized = false

    // Turn buffer for memory extraction
    private val currentTurnText = StringBuilder()

    fun getProfilesFlow(): Flow<List<UserProfileEntity>> =
        repository?.allProfiles ?: emptyFlow()

    fun getActiveProfileFlow(): Flow<UserProfileEntity?> =
        repository?.activeProfileFlow ?: emptyFlow()

    fun getMemoriesFlow(): Flow<List<MemoryEntity>> =
        repository?.allMemoriesFlow ?: emptyFlow()

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val database = MayraDatabase.getInstance(context)
        repository = MayraMemoryRepository(database.userProfileDao(), database.memoryDao())

        trackPlayer = AudioTrackPlayer(
            onPlaybackStarted = {
                scope.launch {
                    recordManager.isModelCurrentlySpeaking = true
                    _uiState.update {
                        it.copy(
                            assistantStatus = AssistantStatus.Speaking,
                            sassyCaption = "Speaking... Listen carefully, darling."
                        )
                    }
                }
            },
            onPlaybackEnded = {
                scope.launch {
                    recordManager.isModelCurrentlySpeaking = false
                    _uiState.update {
                        it.copy(
                            assistantStatus = AssistantStatus.Listening,
                            mayraAudioLevel = 0f,
                            sassyCaption = MayraSass.getRandomListeningQuote()
                        )
                    }
                }
            },
            onAmplitudeChanged = { level ->
                _uiState.update { it.copy(mayraAudioLevel = level) }
            }
        )

        recordManager = AudioRecordManager(
            onAudioChunk = { chunk ->
                liveClient.sendAudioChunk(chunk)
            },
            onAmplitudeChanged = { level ->
                _uiState.update { it.copy(userAudioLevel = level) }
            },
            onInterruptionDetected = {
                handleInterruption("Voice interruption")
            }
        )

        liveClient = GeminiLiveClient(this)

        // Load saved preferences
        val savedCustomKey = sharedPrefs?.getString(PREF_KEY_API_KEY, "") ?: ""
        val resolvedKey = when {
            savedCustomKey.isNotBlank() -> savedCustomKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        val savedVoiceId = sharedPrefs?.getString(PREF_KEY_VOICE, PersonalityVoice.AOEDE.id) ?: PersonalityVoice.AOEDE.id
        val resolvedVoice = PersonalityVoice.entries.find { it.id == savedVoiceId } ?: PersonalityVoice.AOEDE
        val savedModel = sharedPrefs?.getString(PREF_KEY_MODEL, "gemini-3.1-flash-live-preview") ?: "gemini-3.1-flash-live-preview"
        val isBgEnabled = sharedPrefs?.getBoolean(PREF_KEY_BG, true) ?: true

        _uiState.update {
            it.copy(
                apiKey = resolvedKey,
                hasApiKey = resolvedKey.isNotBlank(),
                activeVoice = resolvedVoice,
                activeModel = savedModel,
                isBackgroundRunning = isBgEnabled
            )
        }
    }

    fun startSession(context: Context) {
        val key = _uiState.value.apiKey
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Error("Gemini API Key is required. Please set it in Settings."),
                    sassyCaption = "Hey, someone forgot to give me my Gemini API key!"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                connectionState = ConnectionState.Connecting(),
                assistantStatus = AssistantStatus.Idle,
                sassyCaption = "Waking up Mayra with your profile & memories..."
            )
        }

        scope.launch {
            val contextPrompt = repository?.buildContextPrompt() ?: ""
            val modelTarget = "models/" + _uiState.value.activeModel
            liveClient.connect(key, modelTarget, _uiState.value.activeVoice.id, contextPrompt)

            if (_uiState.value.isBackgroundRunning) {
                startForegroundService(context)
            }
        }
    }

    fun stopSession(context: Context? = null) {
        trackPlayer.enqueueAudio(com.example.audio.AudioCueGenerator.generateSleepChime(), 24000)
        scope.launch {
            delay(350)
            liveClient.disconnect()
            recordManager.stopRecording()
            trackPlayer.release()
        }

        _uiState.update {
            it.copy(
                connectionState = ConnectionState.Disconnected,
                assistantStatus = AssistantStatus.Idle,
                userAudioLevel = 0f,
                mayraAudioLevel = 0f,
                sassyCaption = "I'm offline. Catch you later, gorgeous."
            )
        }

        context?.let { stopForegroundService(it) }
    }

    fun handleInterruption(reason: String = "User manual trigger") {
        Log.d(TAG, "Handling interruption: $reason")
        trackPlayer.interrupt()
        recordManager.isModelCurrentlySpeaking = false
        // Play instant subtle pop cue
        trackPlayer.enqueueAudio(com.example.audio.AudioCueGenerator.generateInterruptionPop(), 24000)

        _uiState.update {
            it.copy(
                assistantStatus = AssistantStatus.Interrupted,
                mayraAudioLevel = 0f,
                interruptionCount = it.interruptionCount + 1,
                sassyCaption = MayraSass.getRandomInterruptedQuote()
            )
        }

        scope.launch {
            delay(1200)
            if (_uiState.value.connectionState is ConnectionState.Connected) {
                _uiState.update {
                    it.copy(
                        assistantStatus = AssistantStatus.Listening,
                        sassyCaption = MayraSass.getRandomListeningQuote()
                    )
                }
            }
        }
    }

    fun toggleMicMute() {
        val newMuted = !_uiState.value.isMicMuted
        recordManager.isMuted = newMuted
        _uiState.update {
            it.copy(
                isMicMuted = newMuted,
                sassyCaption = if (newMuted) "Mic muted. Giving you your privacy, sweetheart." else "Mic unmuted. Back to business!"
            )
        }
    }

    fun updateApiKey(newKey: String) {
        val trimmed = newKey.trim()
        sharedPrefs?.edit()?.putString(PREF_KEY_API_KEY, trimmed)?.apply()
        _uiState.update {
            it.copy(
                apiKey = trimmed,
                hasApiKey = trimmed.isNotBlank()
            )
        }
    }

    fun updateVoice(voice: PersonalityVoice) {
        sharedPrefs?.edit()?.putString(PREF_KEY_VOICE, voice.id)?.apply()
        _uiState.update { it.copy(activeVoice = voice) }
    }

    fun updateModel(modelName: String) {
        val cleanModel = modelName.trim().removePrefix("models/")
        sharedPrefs?.edit()?.putString(PREF_KEY_MODEL, cleanModel)?.apply()
        _uiState.update { it.copy(activeModel = cleanModel) }
    }

    fun toggleBackgroundMode(context: Context) {
        val newEnabled = !_uiState.value.isBackgroundRunning
        sharedPrefs?.edit()?.putBoolean(PREF_KEY_BG, newEnabled)?.apply()
        _uiState.update { it.copy(isBackgroundRunning = newEnabled) }

        if (newEnabled) {
            if (_uiState.value.connectionState is ConnectionState.Connected) {
                startForegroundService(context)
            }
        } else {
            stopForegroundService(context)
        }
    }

    // --- Profile & Memory Management ---

    fun saveProfile(profile: UserProfileEntity) {
        scope.launch {
            repository?.saveProfile(profile)
        }
    }

    fun activateProfile(profileId: Long) {
        scope.launch {
            repository?.setActiveProfile(profileId)
        }
    }

    fun deleteProfile(profile: UserProfileEntity) {
        scope.launch {
            repository?.deleteProfile(profile)
        }
    }

    fun addMemory(topic: String, fact: String, category: String = "Fact") {
        scope.launch {
            repository?.addMemory(topic, fact, category)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        scope.launch {
            repository?.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        scope.launch {
            repository?.clearAllMemories()
        }
    }

    private fun startForegroundService(context: Context) {
        try {
            val intent = Intent(context, MayraVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MayraVoiceService: ${e.message}", e)
        }
    }

    private fun stopForegroundService(context: Context) {
        try {
            val intent = Intent(context, MayraVoiceService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MayraVoiceService: ${e.message}", e)
        }
    }

    // --- GeminiLiveListener Implementation ---

    override fun onConnected() {
        scope.launch {
            // Play Mayra's signature arrival acoustic cue
            trackPlayer.enqueueAudio(com.example.audio.AudioCueGenerator.generateWakeChime(), 24000)

            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Connected,
                    assistantStatus = AssistantStatus.Listening,
                    sassyCaption = MayraSass.getRandomListeningQuote()
                )
            }

            val recordStarted = recordManager.startRecording()
            if (!recordStarted) {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Error("Microphone permission or hardware initialization failed."),
                        sassyCaption = "I can't hear you! Did you give me mic permission?"
                    )
                }
            } else {
                delay(400)
                liveClient.sendInitialGreetingPrompt()
            }
        }
    }

    override fun onAudioDataReceived(pcmData: ByteArray, sampleRate: Int) {
        trackPlayer.enqueueAudio(pcmData, sampleRate)
    }

    override fun onTextReceived(text: String) {
        scope.launch {
            currentTurnText.append(text)
            _uiState.update {
                it.copy(lastRecognizedText = text)
            }
        }
    }

    override fun onTurnComplete() {
        scope.launch {
            val turnOutput = currentTurnText.toString().trim()
            if (turnOutput.isNotBlank()) {
                repository?.logInteraction(
                    userExcerpt = "Voice Query",
                    assistantExcerpt = turnOutput,
                    topicTag = "Live Dialogue"
                )
                currentTurnText.clear()
            }
        }
    }

    override fun onInterrupted() {
        scope.launch {
            handleInterruption("Server reported interruption")
        }
    }

    override fun onError(errorMessage: String) {
        scope.launch {
            recordManager.stopRecording()
            trackPlayer.release()
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.Error(errorMessage),
                    assistantStatus = AssistantStatus.Idle,
                    sassyCaption = "Oops! Network hiccup or key issue. Let's try that again."
                )
            }
        }
    }

    override fun onClosed() {
        scope.launch {
            if (_uiState.value.connectionState is ConnectionState.Connected) {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Disconnected,
                        assistantStatus = AssistantStatus.Idle,
                        sassyCaption = "Session closed. Tap to reconnect!"
                    )
                }
            }
        }
    }
}
