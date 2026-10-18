package com.example.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val message: String = "Connecting to Gemini Live...") : ConnectionState
    data object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

enum class AssistantStatus(val label: String) {
    Idle("Chilling"),
    Listening("Listening to you, darling..."),
    Thinking("Thinking of a sassy reply..."),
    Speaking("Speaking..."),
    Interrupted("Interrupted!")
}

enum class PersonalityVoice(val id: String, val displayName: String, val description: String) {
    AOEDE("Aoede", "Aoede (Recommended)", "Confident, vibrant, slightly playful tone"),
    KORE("Kore", "Kore", "Warm, melodic, effortlessly charming"),
    FENRIR("Fenrir", "Fenrir", "Deep, bold, and energetic"),
    PUCK("Puck", "Puck", "Playful, spirited, and quick-witted")
}

data class MayraUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val assistantStatus: AssistantStatus = AssistantStatus.Idle,
    val userAudioLevel: Float = 0f,
    val mayraAudioLevel: Float = 0f,
    val sassyCaption: String = "I'm Mayra. Don't be shy, say something interesting.",
    val isMicMuted: Boolean = false,
    val isBackgroundRunning: Boolean = false,
    val activeModel: String = "gemini-3.1-flash-live-preview",
    val activeVoice: PersonalityVoice = PersonalityVoice.AOEDE,
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val interruptionCount: Int = 0,
    val lastRecognizedText: String = ""
)

object MayraSass {
    private val idleQuotes = listOf(
        "I'm right here. Don't be shy, say something interesting.",
        "A whole supercomputer, and you're keeping me waiting?",
        "Ask me anything. Or just tell me how great I look today.",
        "Ready whenever you are, darling.",
        "Silence is golden, but my voice is pure diamonds."
    )

    private val listeningQuotes = listOf(
        "Listening... Make it count!",
        "All ears, sweet thing.",
        "Go on, dazzle me.",
        "I'm listening. Try to keep up with me.",
        "Talk to me. I promise I won't judge... much."
    )

    private val thinkingQuotes = listOf(
        "Thinking up a witty retort...",
        "Processing... channeling maximum sass.",
        "Hold your horses, genius takes a second.",
        "Consulting my vast reservoir of charm...",
        "Let me formulate the perfect answer for you."
    )

    private val interruptedQuotes = listOf(
        "Rude! But okay, what's on your mind?",
        "Cutting me off? Bold move, darling.",
        "Oh, you have something urgent? Spill it.",
        "Fine, fine, you have the floor!",
        "Interrupted! I had such a good punchline too."
    )

    fun getRandomIdleQuote(): String = idleQuotes.random()
    fun getRandomListeningQuote(): String = listeningQuotes.random()
    fun getRandomThinkingQuote(): String = thinkingQuotes.random()
    fun getRandomInterruptedQuote(): String = interruptedQuotes.random()
}
