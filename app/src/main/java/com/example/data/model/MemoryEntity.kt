package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val fact: String,
    val category: String = "General", // "Preference", "Fact", "Topic", "InsideJoke"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "interaction_logs")
data class InteractionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userExcerpt: String,
    val assistantExcerpt: String,
    val topicTag: String,
    val timestamp: Long = System.currentTimeMillis()
)
