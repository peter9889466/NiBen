package com.niben.app.quiz

import android.content.Context
import com.niben.app.data.CategoryRatioStore
import com.niben.app.data.ContentCategory
import com.niben.app.data.ContentDao
import com.niben.app.data.ContentItem
import com.niben.app.data.ItemWithLogStatus
import com.niben.app.data.LevelFilterStore
import com.niben.app.data.QuizType
import kotlinx.coroutines.flow.first
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
     * context가 주어지면 CategoryRatioStore의 카테고리 가중치와 LevelFilterStore의 난이도 필터,
     * 그리고 SRS 및 오답 가중치를 종합 반영하여 문제를 선택한다.
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
     * 난이도 설정과 가중치(SRS + 오답 빈도)를 조합하여 최종 문제를 1개 선택한다.
     */
    private suspend fun pickItem(dao: ContentDao, excludeIds: List<Long>, context: Context?): ContentItem? {
        if (context == null) {
            return pickAnyCategoryFallback(dao, excludeIds, listOf(1, 2, 3, 4, 5))
        }

        // 1. DataStore에서 활성화된 난이도(levels) 목록 획득
        val levels = LevelFilterStore.getSelectedLevelsFlow(context).first().toList()

        // 2. 카테고리 가중치 기반으로 카테고리 1개 결정
        val category = CategoryRatioStore.pickWeightedCategory(CategoryRatioStore.getWeights(context))
            ?: return pickAnyCategoryFallback(dao, excludeIds, levels)

        // 3. 해당 카테고리와 난이도 조건에 맞는 단어 목록 + 퀴즈 통계 정보 조회
        val candidates = dao.getItemsWithLogStatus(category, levels)
        if (candidates.isEmpty()) {
            // 해당 카테고리에 조건 만족 데이터가 없으면 타 카테고리 fallback
            return pickAnyCategoryFallback(dao, excludeIds, levels)
        }

        // 4. 각 후보의 SRS 및 오답 가중치 계산
        val weightedItems = candidates.map { candidate ->
            val weight = calculateSrsWeight(candidate, excludeIds)
            candidate to weight
        }

        // 5. 가중치 합산 기반 룰렛 휠 선택
        val totalWeight = weightedItems.sumOf { it.second }
        if (totalWeight <= 0.0) {
            // 모든 가중치가 0인 경우(예: 모든 단어가 최근 출제 제외 리스트에 묶임 등)
            // 최근 제외 조건을 무시하고 가중치 재계산하여 선택
            val fallbackWeightedItems = candidates.map { candidate ->
                val weight = calculateSrsWeight(candidate, emptyList())
                candidate to weight
            }
            val fallbackTotalWeight = fallbackWeightedItems.sumOf { it.second }
            if (fallbackTotalWeight <= 0.0) {
                return candidates.random().toContentItem()
            }
            return selectWeightedRandom(fallbackWeightedItems, fallbackTotalWeight)?.toContentItem()
        }

        return selectWeightedRandom(weightedItems, totalWeight)?.toContentItem()
    }

    /**
     * SRS 및 오답 이력을 활용하여 아이템의 가중치를 계산하는 로직
     */
    private fun calculateSrsWeight(item: ItemWithLogStatus, excludeIds: List<Long>): Double {
        // 최근에 출제된 문제(최근 15개)는 노출 배제
        if (item.id in excludeIds) {
            return 0.0
        }

        var weight = 10.0 // 기본 가중치

        if (item.totalCount == 0) {
            // 한 번도 안 풀어본 문제: 우선 학습할 수 있게 가중치 추가
            weight += 15.0
        } else {
            val now = System.currentTimeMillis()
            val elapsedMs = now - (item.lastAnsweredAt ?: 0L)

            if (item.isLastCorrect == false) {
                // 마지막 풀이 오답: 즉시 복습을 위해 높은 우선순위 부여
                weight += 30.0

                // SRS 시간 경과 반영:
                // 너무 바로(2분 이내) 나오진 않도록 하되, 2분 ~ 24시간 이내라면 복습 타이밍이므로 추가 가중치 부여
                val elapsedMinutes = elapsedMs / (60 * 1000)
                if (elapsedMinutes in 2..1440) {
                    weight += 20.0
                }
            } else {
                // 마지막 풀이 정답: 간격 반복 효과로 노출 주기 확대
                // 최근 24시간 이내에 이미 맞혔던 문제라면 노출 확률 대폭 삭감
                val elapsedHours = elapsedMs / (60 * 60 * 1000)
                if (elapsedHours < 24) {
                    weight = 2.0
                }
            }

            // 누적 오답 횟수가 클수록 가중치 누적 증가 (취약 단어 집중 학습)
            weight += item.incorrectCount * 5.0
        }

        return weight
    }

    /**
     * 룰렛 휠 선택 알고리즘
     */
    private fun selectWeightedRandom(
        items: List<Pair<ItemWithLogStatus, Double>>,
        totalWeight: Double
    ): ItemWithLogStatus? {
        val roll = Random.nextDouble(totalWeight)
        var cursor = 0.0
        for ((item, weight) in items) {
            cursor += weight
            if (roll <= cursor) {
                return item
            }
        }
        return items.lastOrNull()?.first
    }

    /**
     * 카테고리에 조건 만족 데이터가 부족할 때 전체 카테고리에서 난이도 조건만 매칭해 fallback 출제
     */
    private suspend fun pickAnyCategoryFallback(
        dao: ContentDao,
        excludeIds: List<Long>,
        levels: List<Int>
    ): ContentItem? {
        // 전체 카테고리에 대해 활성화 난이도 매칭되는 아이템들을 모두 가져와 SRS 가중치 태움
        val allCandidates = mutableListOf<ItemWithLogStatus>()
        for (category in ContentCategory.entries) {
            allCandidates.addAll(dao.getItemsWithLogStatus(category, levels))
        }

        if (allCandidates.isEmpty()) {
            // 만약 난이도 필터링을 통과한 데이터가 아예 없다면(가상 시나리오), 난이도 조건까지 풀어버리고 무작위 반환
            return dao.getRandomItemExcludingIds(excludeIds) ?: dao.getRandomItem()
        }

        val weightedItems = allCandidates.map { candidate ->
            val weight = calculateSrsWeight(candidate, excludeIds)
            candidate to weight
        }

        val totalWeight = weightedItems.sumOf { it.second }
        if (totalWeight <= 0.0) {
            return allCandidates.random().toContentItem()
        }

        return selectWeightedRandom(weightedItems, totalWeight)?.toContentItem()
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
