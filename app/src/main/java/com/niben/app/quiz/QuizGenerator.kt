package com.niben.app.quiz

import com.niben.app.data.ContentCategory
import com.niben.app.data.ContentDao
import kotlin.random.Random

data class OxQuiz(
    val itemId: Long,
    val questionText: String,
    val correctAnswer: Boolean
)

object QuizGenerator {
    suspend fun generateOx(dao: ContentDao): OxQuiz? {
        val item = dao.getRandomItem() ?: return null
        val useCorrectPairing = Random.nextBoolean()
        val displayAnswer = if (useCorrectPairing) {
            item.meaningKo
        } else {
            dao.getRandomItemExcept(item.category, item.id)?.meaningKo ?: item.meaningKo
        }
        return OxQuiz(
            itemId = item.id,
            questionText = statementFor(item.category, item.japaneseText, displayAnswer),
            correctAnswer = displayAnswer == item.meaningKo
        )
    }

    private fun statementFor(category: ContentCategory, japaneseText: String, answer: String): String =
        when (category) {
            ContentCategory.HIRAGANA, ContentCategory.KATAKANA ->
                "「$japaneseText」의 로마자 표기는 '$answer'이다"
            else ->
                "「$japaneseText」는 '$answer'라는 뜻이다"
        }
}
