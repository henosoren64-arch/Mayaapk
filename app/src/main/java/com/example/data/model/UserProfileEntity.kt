package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileName: String,
    val userNickname: String = "Darling",
    val sassLevel: String = "Playful & Sassy", // "Playful & Sassy", "Sharp & Teasing", "Sweet & Confident", "Sarcastic Genius"
    val topicsOfInterest: String = "Technology, AI, Music, Pop Culture",
    val commonPhrases: String = "Keep it snappy, Coffee first",
    val customWittyRemarks: String = "You're my favorite human, You know you're obsessed with me",
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
