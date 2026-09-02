package com.niben.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * level: 1(N5, 가장 쉬움) ~ 5(N1, 가장 어려움) 매핑. Phase 1부터 저장만 하고,
 * 난이도 필터 UI는 Phase 4에서 노출한다(TASK.md 참고).
 */
@Entity(tableName = "content_item")
data class ContentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: ContentCategory,
    val japaneseText: String,
    val reading: String,
    val meaningKo: String,
    val level: Int? = null,
    val exampleSentence: String? = null,
    val source: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)

