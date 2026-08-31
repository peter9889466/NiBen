package com.niben.app.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceCompletionTest {

    @Test
    fun testSentenceBlankReplacement() {
        val sentence = "この電車は新宿駅に行きますか。"
        val targetWord = "電車"
        val replaced = sentence.replaceFirst(targetWord, "( ___ )")
        assertEquals("この( ___ )は新宿駅に行きますか。", replaced)
    }

    @Test
    fun testKanjiExtractionRegex() {
        val sentence = "空港までお願いします。"
        val kanjiChunks = Regex("""[\p{IsHan}]{1,4}""").findAll(sentence).map { it.value }.toList()
        assertTrue(kanjiChunks.contains("空港"))
        assertTrue(kanjiChunks.contains("願"))
    }
}
