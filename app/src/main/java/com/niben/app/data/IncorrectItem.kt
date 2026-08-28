package com.niben.app.data

/**
 * 오답노트 리스트 뷰에서 각 단어의 오답 통계를 보여주기 위한 DTO 클래스입니다.
 */
data class IncorrectItem(
    val id: Long,
    val category: ContentCategory,
    val japaneseText: String,
    val reading: String,
    val meaningKo: String,
    val level: Int?,
    val incorrectCount: Int,
    val totalCount: Int
)
