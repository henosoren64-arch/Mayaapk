package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MemoryEntity
import com.example.data.model.UserProfileEntity
import com.example.model.MayraUiState
import com.example.model.PersonalityVoice
import com.example.session.MayraSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MayraViewModel : ViewModel() {

    val uiState: StateFlow<MayraUiState> = MayraSessionManager.uiState

    val profiles: StateFlow<List<UserProfileEntity>> = MayraSessionManager.getProfilesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeProfile: StateFlow<UserProfileEntity?> = MayraSessionManager.getActiveProfileFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val memories: StateFlow<List<MemoryEntity>> = MayraSessionManager.getMemoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startSession(context: Context) {
        MayraSessionManager.startSession(context)
    }

    fun stopSession(context: Context) {
        MayraSessionManager.stopSession(context)
    }

    fun toggleMute() {
        MayraSessionManager.toggleMicMute()
    }

    fun handleInterruption(reason: String = "Manual UI Tap") {
        MayraSessionManager.handleInterruption(reason)
    }

    fun saveProfile(profile: UserProfileEntity) {
        MayraSessionManager.saveProfile(profile)
    }

    fun selectActiveProfile(profileId: Long) {
        MayraSessionManager.activateProfile(profileId)
    }

    fun deleteProfile(profile: UserProfileEntity) {
        MayraSessionManager.deleteProfile(profile)
    }

    fun addMemory(topic: String, fact: String, category: String) {
        MayraSessionManager.addMemory(topic, fact, category)
    }

    fun deleteMemory(memory: MemoryEntity) {
        MayraSessionManager.deleteMemory(memory)
    }

    fun clearAllMemories() {
        MayraSessionManager.clearAllMemories()
    }

    fun updateApiKey(key: String) {
        MayraSessionManager.updateApiKey(key)
    }

    fun updateVoice(voice: PersonalityVoice) {
        MayraSessionManager.updateVoice(voice)
    }

    fun updateModel(model: String) {
        MayraSessionManager.updateModel(model)
    }

    fun toggleBackgroundMode(context: Context) {
        MayraSessionManager.toggleBackgroundMode(context)
    }
}
