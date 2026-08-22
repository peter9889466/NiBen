package com.niben.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ContentItem>)

    @Query("SELECT * FROM content_item WHERE id = :id")
    suspend fun getById(id: Long): ContentItem?

    @Query("SELECT * FROM content_item WHERE category = :category")
    suspend fun getByCategory(category: ContentCategory): List<ContentItem>

    @Query("SELECT COUNT(*) FROM content_item")
    suspend fun count(): Int

    @Query("SELECT * FROM content_item ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomItem(): ContentItem?

    @Query("SELECT * FROM content_item WHERE category = :category AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomItemExcept(category: ContentCategory, excludeId: Long): ContentItem?

    /** 최근 출제된 항목(excludeIds)을 제외하고 무작위로 하나 뽑는다. */
    @Query("SELECT * FROM content_item WHERE id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomItemExcludingIds(excludeIds: List<Long>): ContentItem?

    /** 오답 보기(distractor) 생성용 — 같은 카테고리에서 특정 항목을 제외하고 무작위로 여러 개 뽑는다. */
    @Query("SELECT * FROM content_item WHERE category = :category AND id != :excludeId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomItemsExcept(category: ContentCategory, excludeId: Long, limit: Int): List<ContentItem>
}
