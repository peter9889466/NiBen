package com.niben.app.data

/**
 * 퀴즈 출제 시 각 아이템의 SRS 가중치를 계산하기 위해 필요한
 * ContentItem 정보와 최근 퀴즈 기록 통계를 함께 갖는 DTO 클래스입니다.
 */
data class ItemWithLogStatus(
    val id: Long,
    val category: ContentCategory,
    val japaneseText: String,
    val reading: String,
    val meaningKo: String,
    val level: Int?,
    val exampleSentence: String?,
    val source: String,
    val lastAnsweredAt: Long?,
    val isLastCorrect: Boolean?,
    val incorrectCount: Int,
    val totalCount: Int
) {
    fun toContentItem() = ContentItem(
        id = id,
        category = category,
        japaneseText = japaneseText,
        reading = reading,
        meaningKo = meaningKo,
        level = level,
        exampleSentence = exampleSentence,
        source = source
    )
}
