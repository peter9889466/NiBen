package com.niben.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContentItem::class, QuizLog::class],
    version = 1,
    exportSchema = true
)
abstract class NibenDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun quizLogDao(): QuizLogDao

    companion object {
        @Volatile
        private var instance: NibenDatabase? = null

        fun getInstance(context: Context): NibenDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NibenDatabase::class.java,
                    "niben.db"
                ).build().also { instance = it }
            }
    }
}
