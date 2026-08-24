package com.niben.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.categoryRatioDataStore by preferencesDataStore(name = "category_ratio")

/**
 * 카테고리(히라가나/가타카나/한자/단어/문장)별 출제 비율(가중치)을 DataStore에 저장한다.
 * 가중치가 클수록 그 카테고리가 더 자주 출제된다. 기본값은 모든 카테고리 동일(20).
 */
object CategoryRatioStore {
    const val DEFAULT_WEIGHT = 20
    const val MIN_WEIGHT = 0
    const val MAX_WEIGHT = 100

    private fun keyFor(category: ContentCategory) = intPreferencesKey("weight_${category.name}")

    suspend fun getWeights(context: Context): Map<ContentCategory, Int> {
        val prefs = context.categoryRatioDataStore.data.first()
        return ContentCategory.entries.associateWith { category ->
            prefs[keyFor(category)] ?: DEFAULT_WEIGHT
        }
    }

    suspend fun setWeight(context: Context, category: ContentCategory, weight: Int) {
        val clamped = weight.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
        context.categoryRatioDataStore.edit { prefs ->
            prefs[keyFor(category)] = clamped
        }
    }

    /** 가중치에 비례해 카테고리 하나를 무작위로 고른다. 모든 가중치가 0이면 null. */
    fun pickWeightedCategory(weights: Map<ContentCategory, Int>): ContentCategory? {
        val total = weights.values.sum()
        if (total <= 0) return null
        var roll = (0 until total).random()
        for ((category, weight) in weights) {
            if (weight <= 0) continue
            if (roll < weight) return category
            roll -= weight
        }
        return null
    }
}
