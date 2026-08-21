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
}
