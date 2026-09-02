package com.niben.app.data

data class CategoryStat(
    val category: ContentCategory,
    val totalCount: Int,
    val correctCount: Int,
    val accuracyPercent: Int
)

data class QuizTypeStat(
    val quizType: QuizType,
    val totalCount: Int,
    val correctCount: Int,
    val accuracyPercent: Int
)

data class DailyActivity(
    val dateLabel: String,
    val count: Int
)

data class StudyStatistics(
    val totalSolved: Int,
    val totalCorrect: Int,
    val overallAccuracyPercent: Int,
    val streakDays: Int,
    val todaySolved: Int,
    val categoryStats: List<CategoryStat>,
    val typeStats: List<QuizTypeStat>,
    val recent7DaysActivity: List<DailyActivity>
)
