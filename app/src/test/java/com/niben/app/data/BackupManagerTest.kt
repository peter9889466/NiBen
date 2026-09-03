package com.niben.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupManagerTest {

    @Test
    fun parseSummary_validJson_extractsMetadataCorrectly() {
        val json = """
            {
              "app": "NiBen",
              "version": 1,
              "exportedAt": 1725350000000,
              "customWords": [
                {
                  "category": "VOCAB",
                  "japaneseText": "猫",
                  "reading": "ねこ",
                  "meaningKo": "고양이",
                  "isCustom": true
                },
                {
                  "category": "KANJI",
                  "japaneseText": "犬",
                  "reading": "いぬ",
                  "meaningKo": "개",
                  "isCustom": true
                }
              ],
              "favorites": [
                {
                  "category": "VOCAB",
                  "japaneseText": "猫",
                  "reading": "ねこ",
                  "meaningKo": "고양이"
                }
              ],
              "quizLogs": [
                {
                  "itemId": 1,
                  "quizType": "OX",
                  "isCorrect": true,
                  "answeredAt": 1725351000000
                },
                {
                  "itemId": 2,
                  "quizType": "FOUR_CHOICE",
                  "isCorrect": false,
                  "answeredAt": 1725352000000
                },
                {
                  "itemId": 1,
                  "quizType": "MEANING_INPUT",
                  "isCorrect": true,
                  "answeredAt": 1725353000000
                }
              ]
            }
        """.trimIndent()

        val summary = BackupManager.parseSummary(json)

        assertNotNull(summary)
        assertEquals(2, summary?.customWordCount)
        assertEquals(1, summary?.favoriteCount)
        assertEquals(3, summary?.quizLogCount)
        assertEquals(1725350000000L, summary?.exportedAt)
    }

    @Test
    fun parseSummary_invalidAppIdentifier_returnsNull() {
        val json = """
            {
              "app": "OtherApp",
              "version": 1,
              "customWords": []
            }
        """.trimIndent()

        val summary = BackupManager.parseSummary(json)
        assertNull(summary)
    }

    @Test
    fun parseSummary_malformedJson_returnsNull() {
        val malformedJson = "{ invalid json content ... "
        val summary = BackupManager.parseSummary(malformedJson)
        assertNull(summary)
    }
}
