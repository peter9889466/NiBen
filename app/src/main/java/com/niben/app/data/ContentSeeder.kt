package com.niben.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * 앱 최초 실행 시 히라가나/가타카나 전체 표 + assets/vocab_seed.json을
 * Room content_item 테이블에 채운다. 이미 데이터가 있으면 아무 것도 하지 않는다.
 */
object ContentSeeder {
    suspend fun seedIfEmpty(context: Context, dao: ContentDao) {
        if (dao.count() > 0) return
        val items = buildList {
            addAll(KanaTable.hiraganaItems())
            addAll(KanaTable.katakanaItems())
            addAll(loadVocabFromAssets(context))
        }
        dao.insertAll(items)
    }

    private suspend fun loadVocabFromAssets(context: Context): List<ContentItem> =
        withContext(Dispatchers.IO) {
            val json = context.assets.open("vocab_seed.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ContentItem(
                    category = ContentCategory.VOCAB,
                    japaneseText = obj.getString("japaneseText"),
                    reading = obj.getString("reading"),
                    meaningKo = obj.getString("meaningKo"),
                    level = if (obj.has("level")) obj.getInt("level") else null,
                    exampleSentence = obj.optString("exampleSentence").ifBlank { null },
                    source = obj.getString("source")
                )
            }
        }
}
