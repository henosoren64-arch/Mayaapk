package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.MemoryDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.InteractionLogEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        MemoryEntity::class,
        InteractionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MayraDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MayraDatabase? = null

        fun getInstance(context: Context): MayraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MayraDatabase::class.java,
                    "mayra_assistant_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default profile and initial memories
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.userProfileDao().insertProfile(
                                    UserProfileEntity(
                                        profileName = "Playful Companion",
                                        userNickname = "Darling",
                                        sassLevel = "Playful & Sassy",
                                        topicsOfInterest = "Technology, AI, Coffee, Music, Weekend Plans",
                                        commonPhrases = "Always keep it 100, Coffee before conversation",
                                        customWittyRemarks = "You know you secretly can't live without me, Don't make that face at your screen",
                                        isActive = true
                                    )
                                )

                                database.memoryDao().insertMemory(
                                    MemoryEntity(
                                        topic = "Persona Preference",
                                        fact = "Prefers witty banter, clever callbacks, and lively energy over stiff robotic answers.",
                                        category = "Preference"
                                    )
                                )
                                database.memoryDao().insertMemory(
                                    MemoryEntity(
                                        topic = "Favorite Vibe",
                                        fact = "Enjoys friendly teasing and sharp, punchy one-liners.",
                                        category = "Preference"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
