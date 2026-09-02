package com.niben.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContentItem::class, QuizLog::class],
    version = 2,
    exportSchema = true
)
abstract class NibenDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun quizLogDao(): QuizLogDao

    companion object {
        @Volatile
        private var instance: NibenDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content_item ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE content_item ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): NibenDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NibenDatabase::class.java,
                    "niben.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
    }
}

