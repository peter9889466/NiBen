package com.niben.app.quiz

import com.niben.app.data.ContentCategory
import com.niben.app.data.ContentDao
import com.niben.app.data.QuizType
import kotlin.random.Random

data class OxQuiz(
    val itemId: Long,
    val questionText: String,
    val correctAnswer: Boolean
)

data class MultipleChoiceQuiz(
    val itemId: Long,
    val quizType: QuizType,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int
)

object QuizGenerator {
    /** excludeIds에 담긴 항목(최근 출제분)은 제외하고 무작위 OX 문제를 만든다. */
    suspend fun generateOx(dao: ContentDao, excludeIds: List<Long> = emptyList()): OxQuiz? {
        val item = pickItem(dao, excludeIds) ?: return null
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

    /** choiceCount(3 또는 4)개의 선택지를 가진 퀴즈를 만든다. 오답 보기는 같은 카테고리에서 뽑는다. */
    suspend fun generateMultipleChoice(
        dao: ContentDao,
        choiceCount: Int,
        excludeIds: List<Long> = emptyList()
    ): MultipleChoiceQuiz? {
        val item = pickItem(dao, excludeIds) ?: return null
        val distractors = dao.getRandomItemsExcept(item.category, item.id, choiceCount - 1)
            .map { it.meaningKo }
            .distinct()
            .filter { it != item.meaningKo }
            .take(choiceCount - 1)
        if (distractors.size < choiceCount - 1) return null

        val options = (distractors + item.meaningKo).shuffled()
        val correctIndex = options.indexOf(item.meaningKo)
        val quizType = if (choiceCount >= 4) QuizType.FOUR_CHOICE else QuizType.THREE_CHOICE

        return MultipleChoiceQuiz(
            itemId = item.id,
            quizType = quizType,
            questionText = questionFor(item.category, item.japaneseText),
            options = options,
            correctIndex = correctIndex
        )
    }

    private suspend fun pickItem(dao: ContentDao, excludeIds: List<Long>) =
        if (excludeIds.isEmpty()) {
            dao.getRandomItem()
        } else {
            dao.getRandomItemExcludingIds(excludeIds) ?: dao.getRandomItem()
        }

    private fun statementFor(category: ContentCategory, japaneseText: String, answer: String): String =
        when (category) {
            ContentCategory.HIRAGANA, ContentCategory.KATAKANA ->
                "「$japaneseText」의 로마자 표기는 '$answer'이다"
            else ->
                "「$japaneseText」는 '$answer'라는 뜻이다"
        }

    private fun questionFor(category: ContentCategory, japaneseText: String): String =
        when (category) {
            ContentCategory.HIRAGANA, ContentCategory.KATAKANA ->
                "「$japaneseText」의 로마자 표기는?"
            else ->
                "「$japaneseText」의 뜻은?"
        }
}
