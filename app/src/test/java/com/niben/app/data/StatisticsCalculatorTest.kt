package com.niben.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatisticsCalculatorTest {

    private val zoneId = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 9, 2)

    @Test
    fun `calculateStreak returns 0 when timestamps is empty`() {
        val streak = StatisticsCalculator.calculateStreak(emptyList(), today, zoneId)
        assertEquals(0, streak)
    }

    @Test
    fun `calculateStreak returns 1 when solved only today`() {
        val todayMillis = today.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val streak = StatisticsCalculator.calculateStreak(listOf(todayMillis), today, zoneId)
        assertEquals(1, streak)
    }

    @Test
    fun `calculateStreak preserves streak when solved yesterday but not yet today`() {
        val yesterdayMillis = today.minusDays(1).atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
        val streak = StatisticsCalculator.calculateStreak(listOf(yesterdayMillis), today, zoneId)
        assertEquals(1, streak)
    }

    @Test
    fun `calculateStreak counts multiple consecutive days correctly`() {
        val day0 = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val day1 = today.minusDays(1).atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()
        val day2 = today.minusDays(2).atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
        val day3 = today.minusDays(3).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 4일 연속
        val streak = StatisticsCalculator.calculateStreak(listOf(day3, day2, day1, day0), today, zoneId)
        assertEquals(4, streak)
    }

    @Test
    fun `calculateStreak breaks when a day is skipped`() {
        val day0 = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val day1 = today.minusDays(1).atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()
        // minusDays(2) 누락됨
        val day3 = today.minusDays(3).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        val streak = StatisticsCalculator.calculateStreak(listOf(day3, day1, day0), today, zoneId)
        assertEquals(2, streak)
    }

    @Test
    fun `calculateStreak returns 0 when solved two days ago but not yesterday or today`() {
        val day2 = today.minusDays(2).atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
        val streak = StatisticsCalculator.calculateStreak(listOf(day2), today, zoneId)
        assertEquals(0, streak)
    }
}
