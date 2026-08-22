package com.niben.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.recentItemsDataStore by preferencesDataStore(name = "recent_items")

/**
 * 최근에 출제된 content_item id를 DataStore에 보관해, 같은 문제가 연달아
 * 나오지 않도록 QuizGenerator의 제외 목록으로 사용한다.
 */
object RecentItemsStore {
    private const val MAX_RECENT = 15
    private val KEY = stringPreferencesKey("recent_item_ids")

    suspend fun getRecentIds(context: Context): List<Long> {
        val raw = context.recentItemsDataStore.data.first()[KEY].orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    suspend fun recordShown(context: Context, itemId: Long) {
        context.recentItemsDataStore.edit { prefs ->
            val current = prefs[KEY].orEmpty()
                .split(",")
                .mapNotNull { it.toLongOrNull() }
            val updated = (listOf(itemId) + current.filter { it != itemId }).take(MAX_RECENT)
            prefs[KEY] = updated.joinToString(",")
        }
    }
}
