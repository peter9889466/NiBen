package com.niben.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.levelFilterDataStore by preferencesDataStore(name = "level_filter")

/**
 * 난이도(1~5)별 출제 활성화 여부를 Preferences DataStore에 저장한다.
 * 기본값은 모든 난이도가 활성화(true)되어 있는 상태이다.
 */
object LevelFilterStore {
    private fun keyFor(level: Int) = booleanPreferencesKey("level_$level")

    /** 활성화된 난이도 목록을 Set으로 가져오는 Flow */
    fun getSelectedLevelsFlow(context: Context): Flow<Set<Int>> {
        return context.levelFilterDataStore.data.map { prefs ->
            val selected = mutableSetOf<Int>()
            for (level in 1..5) {
                // 기본값은 true (활성화)
                val isEnabled = prefs[keyFor(level)] ?: true
                if (isEnabled) {
                    selected.add(level)
                }
            }
            // 유효성 보장: 만약 아무 것도 선택되지 않은 상태라면 기본값으로 1을 반환
            if (selected.isEmpty()) setOf(1) else selected
        }
    }

    /** 특정 난이도의 활성화 여부를 설정 */
    suspend fun setLevelEnabled(context: Context, level: Int, enabled: Boolean) {
        if (level !in 1..5) return
        context.levelFilterDataStore.edit { prefs ->
            prefs[keyFor(level)] = enabled
        }
    }
}
