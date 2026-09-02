package com.niben.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object StatisticsCalculator {

    suspend fun calculate(
        quizLogDao: QuizLogDao,
        zoneId: ZoneId = ZoneId.systemDefault(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): StudyStatistics {
        val totalSolved = quizLogDao.getTotalSolvedCount()
        val totalCorrect = quizLogDao.getTotalCorrectCount()
        val overallAccuracy = if (totalSolved > 0) (totalCorrect * 100 / totalSolved) else 0

        val today = Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId).toLocalDate()
        val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val todaySolved = quizLogDao.getTodaySolvedCount(startOfToday)

        val timestamps = quizLogDao.getAllAnsweredTimestamps()
        val streakDays = calculateStreak(timestamps, today, zoneId)

        // 카테고리별 통계 매핑 (모든 카테고리가 0건이라도 목록에 표시되도록 보장)
        val rawCategoryStats = quizLogDao.getCategoryStats().associateBy { it.category }
        val categoryStats = ContentCategory.entries.map { category ->
            val stat = rawCategoryStats[category]
            val total = stat?.totalCount ?: 0
            val correct = stat?.correctCount ?: 0
            val accuracy = if (total > 0) (correct * 100 / total) else 0
            CategoryStat(
                category = category,
                totalCount = total,
                correctCount = correct,
                accuracyPercent = accuracy
            )
        }

        // 퀴즈 유형별 통계
        val rawTypeStats = quizLogDao.getQuizTypeStats().associateBy { it.quizType }
        val typeStats = QuizType.entries.map { type ->
            val stat = rawTypeStats[type]
            val total = stat?.totalCount ?: 0
            val correct = stat?.correctCount ?: 0
            val accuracy = if (total > 0) (correct * 100 / total) else 0
            QuizTypeStat(
                quizType = type,
                totalCount = total,
                correctCount = correct,
                accuracyPercent = accuracy
            )
        }

        // 최근 7일간 일별 학습량
        val recent7Days = calculateRecent7DaysActivity(timestamps, today, zoneId)

        return StudyStatistics(
            totalSolved = totalSolved,
            totalCorrect = totalCorrect,
            overallAccuracyPercent = overallAccuracy,
            streakDays = streakDays,
            todaySolved = todaySolved,
            categoryStats = categoryStats,
            typeStats = typeStats,
            recent7DaysActivity = recent7Days
        )
    }

    /**
     * 연속 학습일(Streak) 계산:
     * - 오늘 학습했으면 오늘부터 과거로 하루씩 연속 여부 확인
     * - 오늘 아직 학습하지 않았더라도 어제 학습했으면 어제부터 과거로 확인 (연속 학습 기록 유지)
     * - 오늘과 어제 모두 학습하지 않았다면 Streak = 0
     */
    fun calculateStreak(
        timestamps: List<Long>,
        currentDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        if (timestamps.isEmpty()) return 0

        val activeDates = timestamps.map { millis ->
            Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        }.toSet()

        var startDate = when {
            activeDates.contains(currentDate) -> currentDate
            activeDates.contains(currentDate.minusDays(1)) -> currentDate.minusDays(1)
            else -> return 0
        }

        var streak = 0
        var checkDate = startDate
        while (activeDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    private fun calculateRecent7DaysActivity(
        timestamps: List<Long>,
        today: LocalDate,
        zoneId: ZoneId
    ): List<DailyActivity> {
        val dateCounts = mutableMapOf<LocalDate, Int>()
        for (i in 0..6) {
            dateCounts[today.minusDays(6L - i)] = 0
        }

        val sevenDaysAgo = today.minusDays(6)
        for (millis in timestamps) {
            val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
            if (!date.isBefore(sevenDaysAgo) && !date.isAfter(today)) {
                dateCounts[date] = (dateCounts[date] ?: 0) + 1
            }
        }

        val formatter = DateTimeFormatter.ofPattern("M/d")
        return (0..6).map { i ->
            val date = today.minusDays(6L - i)
            DailyActivity(
                dateLabel = if (date == today) "오늘" else date.format(formatter),
                count = dateCounts[date] ?: 0
            )
        }
    }
}
