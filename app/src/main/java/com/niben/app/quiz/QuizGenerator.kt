package com.niben.app.quiz

import android.content.Context
import com.niben.app.data.CategoryRatioStore
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
    /**
     * excludeIds에 담긴 항목(최근 출제분)은 제외하고 무작위 OX 문제를 만든다.
     * context가 주어지면 CategoryRatioStore에 저장된 카테고리별 출제 비율을 반영한다.
     */
    suspend fun generateOx(
        dao: ContentDao,
        excludeIds: List<Long> = emptyList(),
        context: Context? = null
    ): OxQuiz? {
        val item = pickItem(dao, excludeIds, context) ?: return null
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
        excludeIds: List<Long> = emptyList(),
        context: Context? = null
    ): MultipleChoiceQuiz? {
        val item = pickItem(dao, excludeIds, context) ?: return null
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

    /**
     * context가 주어지면 CategoryRatioStore의 카테고리별 가중치로 카테고리를 먼저 고른 뒤
     * 그 안에서 무작위 항목을 뽑는다(그 카테고리에 항목이 없으면 전체에서 무작위로 대체).
     * context가 없으면 기존처럼 전체 카테고리에서 균등하게 무작위로 뽑는다.
     */
    private suspend fun pickItem(dao: ContentDao, excludeIds: List<Long>, context: Context?) =
        if (context == null) {
            pickAnyCategory(dao, excludeIds)
        } else {
            val category = CategoryRatioStore.pickWeightedCategory(CategoryRatioStore.getWeights(context))
            val fromCategory = category?.let {
                if (excludeIds.isEmpty()) {
                    dao.getRandomItemInCategory(it)
                } else {
                    dao.getRandomItemInCategoryExcludingIds(it, excludeIds)
                }
            }
            fromCategory ?: pickAnyCategory(dao, excludeIds)
        }

    private suspend fun pickAnyCategory(dao: ContentDao, excludeIds: List<Long>) =
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
