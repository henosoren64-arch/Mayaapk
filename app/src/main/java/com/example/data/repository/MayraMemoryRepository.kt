package com.example.data.repository

import com.example.data.dao.MemoryDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.InteractionLogEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class MayraMemoryRepository(
    private val userProfileDao: UserProfileDao,
    private val memoryDao: MemoryDao
) {
    val allProfiles: Flow<List<UserProfileEntity>> = userProfileDao.getAllProfiles()
    val activeProfileFlow: Flow<UserProfileEntity?> = userProfileDao.getActiveProfileFlow()
    val allMemoriesFlow: Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()
    val recentInteractionsFlow: Flow<List<InteractionLogEntity>> = memoryDao.getRecentInteractionsFlow()

    suspend fun getActiveProfile(): UserProfileEntity? = userProfileDao.getActiveProfile()

    suspend fun saveProfile(profile: UserProfileEntity): Long {
        return if (profile.id == 0L) {
            userProfileDao.insertProfile(profile)
        } else {
            userProfileDao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun setActiveProfile(profileId: Long) {
        userProfileDao.setActiveProfile(profileId)
    }

    suspend fun deleteProfile(profile: UserProfileEntity) {
        userProfileDao.deleteProfile(profile)
    }

    suspend fun addMemory(topic: String, fact: String, category: String = "General"): Long {
        val memory = MemoryEntity(
            topic = topic.trim(),
            fact = fact.trim(),
            category = category
        )
        return memoryDao.insertMemory(memory)
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
    }

    suspend fun logInteraction(userExcerpt: String, assistantExcerpt: String, topicTag: String) {
        if (userExcerpt.isBlank() && assistantExcerpt.isBlank()) return
        memoryDao.insertInteraction(
            InteractionLogEntity(
                userExcerpt = userExcerpt.take(300),
                assistantExcerpt = assistantExcerpt.take(300),
                topicTag = topicTag
            )
        )
    }

    suspend fun buildContextPrompt(): String {
        val activeProfile = userProfileDao.getActiveProfile()
        val memories = memoryDao.getRecentMemories(12)
        val recentLogs = memoryDao.getRecentInteractionsList(5)

        val sb = StringBuilder()
        sb.append("\n\n### USER PROFILE & DYNAMIC CONTEXT ###\n")

        if (activeProfile != null) {
            sb.append("- Call the user by this name/nickname: \"${activeProfile.userNickname}\"\n")
            sb.append("- Persona Style & Sass Level: ${activeProfile.sassLevel}\n")
            if (activeProfile.topicsOfInterest.isNotBlank()) {
                sb.append("- User's Topics of Interest: ${activeProfile.topicsOfInterest}\n")
            }
            if (activeProfile.commonPhrases.isNotBlank()) {
                sb.append("- User's Familiar Phrases/Inside Slang: ${activeProfile.commonPhrases}\n")
            }
            if (activeProfile.customWittyRemarks.isNotBlank()) {
                sb.append("- Custom Witty Remarks & Teasing Inside Jokes to use: ${activeProfile.customWittyRemarks}\n")
            }
        } else {
            sb.append("- User's Preferred Name: Darling\n")
            sb.append("- Persona Style: Playful & Sassy\n")
        }

        if (memories.isNotEmpty()) {
            sb.append("\n### REMEMBERED FACTS & USER MEMORIES ###\n")
            memories.forEach { memory ->
                sb.append("• [${memory.topic}] ${memory.fact}\n")
            }
        }

        if (recentLogs.isNotEmpty()) {
            sb.append("\n### RECENT PRIOR CONVERSATION RECAP ###\n")
            recentLogs.forEach { log ->
                sb.append("• Topic: ${log.topicTag} | User: \"${log.userExcerpt}\" | Mayra: \"${log.assistantExcerpt}\"\n")
            }
        }

        sb.append(
            "\n### MEMORY INSTRUCTIONS ###\n" +
            "1. Seamlessly reference past interactions, topics, and user preferences when natural. " +
            "2. Make playful callbacks to prior conversations to show you remember them. " +
            "3. Do NOT ask repetitive questions about things you already know about the user. " +
            "4. Keep your spoken responses concise, witty, charismatic, and punchy."
        )

        return sb.toString()
    }
}
