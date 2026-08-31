package com.niben.app.quiz

import com.niben.app.data.ContentCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningValidatorTest {

    @Test
    fun testExactMatch() {
        assertTrue(MeaningValidator.isCorrect("먹다", "먹다"))
        assertTrue(MeaningValidator.isCorrect(" 먹다. ", "먹다"))
        assertTrue(MeaningValidator.isCorrect("먹다", "먹다."))
    }

    @Test
    fun testMultipleMeaningsCommaSeparated() {
        val meaning = "나, 저"
        assertTrue(MeaningValidator.isCorrect("나", meaning))
        assertTrue(MeaningValidator.isCorrect("저", meaning))
        assertTrue(MeaningValidator.isCorrect("나, 저", meaning))
        assertTrue(MeaningValidator.isCorrect("나 , 저", meaning))
        assertFalse(MeaningValidator.isCorrect("너", meaning))
    }

    @Test
    fun testMultipleMeaningsSlashSeparated() {
        val meaning = "비싸다 / 높다"
        assertTrue(MeaningValidator.isCorrect("비싸다", meaning))
        assertTrue(MeaningValidator.isCorrect("높다", meaning))
    }

    @Test
    fun testParenthesesRemoval() {
        val meaning = "화장실은 어디예요?(정중한 표현)"
        assertTrue(MeaningValidator.isCorrect("화장실은 어디예요?", meaning))
        assertTrue(MeaningValidator.isCorrect("화장실은 어디예요", meaning))
        assertTrue(MeaningValidator.isCorrect("화장실은 어디예요?(정중한 표현)", meaning))
        assertFalse(MeaningValidator.isCorrect("정중한 표현", meaning))
    }

    @Test
    fun testKanaPronunciationAndRomaji() {
        assertTrue(
            MeaningValidator.isCorrect(
                userInput = "a",
                meaningKo = "a",
                reading = "あ",
                category = ContentCategory.HIRAGANA
            )
        )
        assertTrue(
            MeaningValidator.isCorrect(
                userInput = "A",
                meaningKo = "a",
                reading = "あ",
                category = ContentCategory.HIRAGANA
            )
        )
        assertTrue(
            MeaningValidator.isCorrect(
                userInput = "あ",
                meaningKo = "a",
                reading = "あ",
                category = ContentCategory.HIRAGANA
            )
        )
    }

    @Test
    fun testIncorrectAnswers() {
        assertFalse(MeaningValidator.isCorrect("자다", "먹다"))
        assertFalse(MeaningValidator.isCorrect("", "먹다"))
        assertFalse(MeaningValidator.isCorrect("   ", "먹다"))
    }
}
