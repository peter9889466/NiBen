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

    /** 오답노트용: 오답 횟수가 1회 이상인 항목을 오답 횟수 내림차순, 오답률 내림차순으로 가져온다. */
    @Query("""
        SELECT 
            c.id as id,
            c.category as category,
            c.japaneseText as japaneseText,
            c.reading as reading,
            c.meaningKo as meaningKo,
            c.level as level,
            SUM(CASE WHEN q.isCorrect = 0 THEN 1 ELSE 0 END) as incorrectCount,
            COUNT(q.id) as totalCount
        FROM content_item c
        INNER JOIN quiz_log q ON c.id = q.itemId
        GROUP BY c.id
        HAVING incorrectCount > 0
        ORDER BY incorrectCount DESC, (incorrectCount * 1.0 / totalCount) DESC
    """)
    suspend fun getIncorrectItems(): List<IncorrectItem>

    /** 오답 기록 초기화(외웠음 처리)를 위해 해당 아이템의 퀴즈 로그를 삭제한다. */
    @Query("DELETE FROM quiz_log WHERE itemId = :itemId")
    suspend fun deleteLogsByItemId(itemId: Long)
}

