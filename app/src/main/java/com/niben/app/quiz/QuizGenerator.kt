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

data class MeaningInputQuiz(
    val itemId: Long,
    val category: ContentCategory,
    val questionText: String,
    val japaneseText: String,
    val reading: String,
    val meaningKo: String,
    val acceptedAnswers: List<String>
)

data class SentenceCompletionQuiz(
    val itemId: Long,
    val category: ContentCategory,
    val koreanMeaning: String,
    val sentenceWithBlank: String,
    val correctAnswer: String,
    val options: List<String>,
    val correctIndex: Int,
    val fullSentence: String,
    val fullReading: String
)

object MeaningValidator {
    /**
     * 사용자가 입력한 답변이 정답 목록이나 meaningKo에 부합하는지 검증한다.
     */
    fun isCorrect(
        userInput: String,
        meaningKo: String,
        reading: String = "",
        category: ContentCategory? = null
    ): Boolean {
        val normalizedInput = normalize(userInput)
        if (normalizedInput.isEmpty()) return false

        // 1. 전체 meaningKo 정규화 비교
        if (normalizedInput == normalize(meaningKo)) return true

        // 2. 가나 카테고리인 경우 로마자 또는 발음(reading) 직접 매칭
        if (category == ContentCategory.HIRAGANA || category == ContentCategory.KATAKANA) {
            if (normalizedInput.equals(normalize(reading), ignoreCase = true)) return true
            if (normalizedInput.equals(normalize(meaningKo), ignoreCase = true)) return true
        }

        // 3. 쉼표(,), 슬래시(/), 세미콜론(;), 가운데점(·) 등으로 분리된 각각의 단어 후보 검사
        val candidates = extractAnswerCandidates(meaningKo)
        for (candidate in candidates) {
            val normCandidate = normalize(candidate)
            if (normCandidate.isNotEmpty() && normCandidate == normalizedInput) {
                return true
            }
            // 괄호 제거 후 재비교 (예: "화장실은 어디예요?(정중한 표현)" -> "화장실은 어디예요?")
            val withoutParentheses = normalize(removeParentheses(candidate))
            if (withoutParentheses.isNotEmpty() && withoutParentheses == normalizedInput) {
                return true
            }
        }

        // 4. 입력값에서 괄호를 없앤 것과 후보들 비교
        val inputNoParen = normalize(removeParentheses(userInput))
        for (candidate in candidates) {
            val normCandidate = normalize(removeParentheses(candidate))
            if (normCandidate.isNotEmpty() && normCandidate == inputNoParen) {
                return true
            }
        }

        return false
    }

    fun extractAnswerCandidates(meaningKo: String): List<String> {
        val delimiters = charArrayOf(',', '/', ';', '·', '、', '\n')
        val list = meaningKo.split(*delimiters).map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val noParen = removeParentheses(meaningKo).trim()
        if (noParen.isNotEmpty() && !list.contains(noParen)) {
            list.add(noParen)
            val subNoParen = noParen.split(*delimiters).map { it.trim() }.filter { it.isNotEmpty() }
            for (sub in subNoParen) {
                if (!list.contains(sub)) list.add(sub)
            }
        }
        return list.distinct()
    }

    fun normalize(text: String): String {
        return text.trim()
            .replace(Regex("""\s*,\s*"""), ", ")
            .replace(Regex("""\s*/\s*"""), " / ")
            .replace(Regex("""\s*;\s*"""), "; ")
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[.~!?]+$"""), "") // 문장 끝 부호 제거
            .lowercase()
    }

    fun removeParentheses(text: String): String {
        return text.replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\[[^]]*\]"""), "")
            .replace(Regex("""<[^>]*>"""), "")
            .trim()
    }
}

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
     * 사용자가 직접 뜻을 입력하는 주관식 퀴즈를 생성한다.
     */
    suspend fun generateMeaningInput(
        dao: ContentDao,
        excludeIds: List<Long> = emptyList(),
        context: Context? = null
    ): MeaningInputQuiz? {
        val item = pickItem(dao, excludeIds, context) ?: return null
        val question = when (item.category) {
            ContentCategory.HIRAGANA, ContentCategory.KATAKANA ->
                "「${item.japaneseText}」의 발음(로마자)을 입력하세요"
            else ->
                "「${item.japaneseText}」의 뜻을 입력하세요"
        }
        val accepted = MeaningValidator.extractAnswerCandidates(item.meaningKo)
        return MeaningInputQuiz(
            itemId = item.id,
            category = item.category,
            questionText = question,
            japaneseText = item.japaneseText,
            reading = item.reading,
            meaningKo = item.meaningKo,
            acceptedAnswers = accepted
        )
    }

    /**
     * 문장에서 핵심 단어를 빈칸 ( ___ )으로 가리고, 4지선다로 알맞은 단어를 맞추는 예문 완성형 퀴즈를 생성한다.
     */
    suspend fun generateSentenceCompletion(
        dao: ContentDao,
        excludeIds: List<Long> = emptyList(),
        context: Context? = null
    ): SentenceCompletionQuiz? {
        // 1. SENTENCE 카테고리 아이템 또는 exampleSentence가 있는 아이템 선별
        val sentenceItems = dao.getByCategory(ContentCategory.SENTENCE)
        val exampleItems = dao.getItemsWithExampleSentence()
        val vocabItems = dao.getVocabAndKanjiItems()

        if (sentenceItems.isEmpty() && exampleItems.isEmpty()) return null

        // 최근 출제된 문장 제외 우선 필터링
        val availableSentences = (sentenceItems + exampleItems)
            .filter { it.id !in excludeIds }
            .ifEmpty { sentenceItems + exampleItems }

        val shuffledCandidates = availableSentences.shuffled()

        // 2. 단어와 매칭하여 빈칸을 만들 수 있는 문장 찾기
        for (item in shuffledCandidates) {
            val sentenceText = if (item.category == ContentCategory.SENTENCE) {
                item.japaneseText
            } else {
                item.exampleSentence ?: item.japaneseText
            }

            val koreanMeaning = item.meaningKo
            val reading = item.reading

            // 해당 문장에 포함된 VOCAB/KANJI 단어 탐색 (길이가 긴 단어 우선)
            val matchedWords = vocabItems
                .filter { vocab ->
                    vocab.japaneseText.length >= 2 &&
                            sentenceText.contains(vocab.japaneseText) &&
                            sentenceText != vocab.japaneseText // 문장 전체가 단어 1개인 경우 제외
                }
                .sortedByDescending { it.japaneseText.length }

            val targetWord: String
            if (matchedWords.isNotEmpty()) {
                targetWord = matchedWords.random().japaneseText
            } else {
                // 단어장에 직접 매칭되지 않는 경우, 문장의 핵심 어절 추출 (한자 구문 또는 2~4글자)
                val kanjiChunks = Regex("""[\p{IsHan}]{1,4}""").findAll(sentenceText).map { it.value }.toList()
                if (kanjiChunks.isNotEmpty()) {
                    targetWord = kanjiChunks.random()
                } else {
                    // 한자가 없는 문장의 경우 2~4글자 가나 어절
                    val words = sentenceText.replace(Regex("""[。、！？\s]"""), " ")
                        .split(" ")
                        .filter { it.length in 2..5 }
                    if (words.isEmpty()) continue
                    targetWord = words.random()
                }
            }

            // 빈칸 생성 (첫 번째 일치 부분 치환)
            val sentenceWithBlank = sentenceText.replaceFirst(targetWord, "( ___ )")
            if (sentenceWithBlank == sentenceText) continue

            // 3. 보기 4개 생성 (정답 1개 + 오답 3개)
            val distractors = vocabItems
                .map { it.japaneseText }
                .filter { it != targetWord && it.length in (targetWord.length - 1)..(targetWord.length + 2) }
                .distinct()
                .shuffled()
                .take(3)
                .toMutableList()

            // 보기가 3개 미만이면 전체 vocab에서 채움
            if (distractors.size < 3) {
                val more = vocabItems.map { it.japaneseText }
                    .filter { it != targetWord && it !in distractors }
                    .shuffled()
                    .take(3 - distractors.size)
                distractors.addAll(more)
            }

            if (distractors.size < 3) continue

            val options = (distractors + targetWord).shuffled()
            val correctIndex = options.indexOf(targetWord)

            return SentenceCompletionQuiz(
                itemId = item.id,
                category = item.category,
                koreanMeaning = koreanMeaning,
                sentenceWithBlank = sentenceWithBlank,
                correctAnswer = targetWord,
                options = options,
                correctIndex = correctIndex,
                fullSentence = sentenceText,
                fullReading = reading
            )
        }

        return null
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
