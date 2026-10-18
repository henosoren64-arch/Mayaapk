package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileFlow(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE user_profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun activateProfile(profileId: Long)

    @Transaction
    suspend fun setActiveProfile(profileId: Long) {
        deactivateAll()
        activateProfile(profileId)
    }
}
