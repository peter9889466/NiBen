package com.niben.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizLogDao {
    @Insert
    suspend fun insert(log: QuizLog)

    @Query("SELECT * FROM quiz_log WHERE itemId = :itemId ORDER BY answeredAt DESC")
    suspend fun getByItem(itemId: Long): List<QuizLog>

    @Query("SELECT * FROM quiz_log ORDER BY answeredAt DESC")
    suspend fun getAll(): List<QuizLog>
}
