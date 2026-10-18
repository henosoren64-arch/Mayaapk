package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InteractionLogEntity
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 15): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: InteractionLogEntity): Long

    @Query("SELECT * FROM interaction_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractionsFlow(limit: Int = 20): Flow<List<InteractionLogEntity>>

    @Query("SELECT * FROM interaction_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentInteractionsList(limit: Int = 10): List<InteractionLogEntity>
}
