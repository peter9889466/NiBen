package com.niben.app.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val customWordCount: Int,
    val favoriteCount: Int,
    val quizLogCount: Int,
    val exportedAt: Long
)

data class RestoreResult(
    val success: Boolean,
    val restoredCustomWords: Int = 0,
    val restoredFavorites: Int = 0,
    val restoredLogs: Int = 0,
    val errorMessage: String? = null
)

object BackupManager {
    private const val BACKUP_VERSION = 1
    private const val APP_IDENTIFIER = "NiBen"

    /**
     * Room DB 데이터를 JSON 문자열로 내보낸다.
     */
    suspend fun exportToJson(contentDao: ContentDao, quizLogDao: QuizLogDao): String {
        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("version", BACKUP_VERSION)
        val now = System.currentTimeMillis()
        root.put("exportedAt", now)

        // 1. 사용자 단어 목록
        val customItems = contentDao.getCustomItems()
        val customArray = JSONArray()
        for (item in customItems) {
            val itemObj = JSONObject()
            itemObj.put("id", item.id)
            itemObj.put("category", item.category.name)
            itemObj.put("japaneseText", item.japaneseText)
            itemObj.put("reading", item.reading)
            itemObj.put("meaningKo", item.meaningKo)
            itemObj.put("level", item.level ?: JSONObject.NULL)
            itemObj.put("exampleSentence", item.exampleSentence ?: JSONObject.NULL)
            itemObj.put("source", item.source)
            itemObj.put("isFavorite", item.isFavorite)
            itemObj.put("isCustom", true)
            customArray.put(itemObj)
        }
        root.put("customWords", customArray)

        // 2. 즐겨찾기 목록
        val favorites = contentDao.getFavorites()
        val favArray = JSONArray()
        for (item in favorites) {
            val favObj = JSONObject()
            favObj.put("id", item.id)
            favObj.put("category", item.category.name)
            favObj.put("japaneseText", item.japaneseText)
            favObj.put("reading", item.reading)
            favObj.put("meaningKo", item.meaningKo)
            favObj.put("isCustom", item.isCustom)
            favArray.put(favObj)
        }
        root.put("favorites", favArray)

        // 3. 퀴즈 풀이 이력
        val logs = quizLogDao.getAll()
        val logArray = JSONArray()
        for (log in logs) {
            val logObj = JSONObject()
            logObj.put("itemId", log.itemId)
            logObj.put("quizType", log.quizType.name)
            logObj.put("isCorrect", log.isCorrect)
            logObj.put("answeredAt", log.answeredAt)
            logArray.put(logObj)
        }
        root.put("quizLogs", logArray)

        return root.toString(2)
    }

    /**
     * 백업 JSON 파일의 요약 메타데이터를 미리 파싱한다.
     */
    fun parseSummary(jsonString: String): BackupSummary? {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != APP_IDENTIFIER) return null

            val customCount = root.optJSONArray("customWords")?.length() ?: 0
            val favCount = root.optJSONArray("favorites")?.length() ?: 0
            val logCount = root.optJSONArray("quizLogs")?.length() ?: 0
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            BackupSummary(
                customWordCount = customCount,
                favoriteCount = favCount,
                quizLogCount = logCount,
                exportedAt = exportedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON 문자열을 파싱하여 Room DB에 데이터를 복원(병합)한다.
     */
    suspend fun importFromJson(
        jsonString: String,
        contentDao: ContentDao,
        quizLogDao: QuizLogDao
    ): RestoreResult {
        return try {
            val root = JSONObject(jsonString)
            val app = root.optString("app")
            if (app != APP_IDENTIFIER) {
                return RestoreResult(
                    success = false,
                    errorMessage = "올바른 NiBen 백업 파일 형식이 아닙니다."
                )
            }

            var restoredCustomCount = 0
            var restoredFavCount = 0
            var restoredLogCount = 0

            // 1. 사용자 단어 복원
            val customArray = root.optJSONArray("customWords")
            if (customArray != null) {
                for (i in 0 until customArray.length()) {
                    val obj = customArray.getJSONObject(i)
                    val category = try {
                        ContentCategory.valueOf(obj.getString("category"))
                    } catch (e: Exception) {
                        ContentCategory.VOCAB
                    }
                    val japaneseText = obj.getString("japaneseText")
                    val reading = obj.optString("reading", japaneseText)
                    val meaningKo = obj.getString("meaningKo")
                    val level = if (obj.isNull("level")) null else obj.optInt("level")
                    val exampleSentence = if (obj.isNull("exampleSentence")) null else obj.optString("exampleSentence")
                    val source = obj.optString("source", "USER")
                    val isFavorite = obj.optBoolean("isFavorite", false)

                    val newItem = ContentItem(
                        category = category,
                        japaneseText = japaneseText,
                        reading = reading,
                        meaningKo = meaningKo,
                        level = level,
                        exampleSentence = exampleSentence,
                        source = source,
                        isFavorite = isFavorite,
                        isCustom = true
                    )
                    contentDao.insert(newItem)
                    restoredCustomCount++
                }
            }

            // 2. 즐겨찾기 상태 복원
            val favArray = root.optJSONArray("favorites")
            if (favArray != null) {
                for (i in 0 until favArray.length()) {
                    val obj = favArray.getJSONObject(i)
                    val japaneseText = obj.getString("japaneseText")
                    val candidates = contentDao.searchItems(japaneseText)
                    val matched = candidates.firstOrNull { it.japaneseText == japaneseText }
                    if (matched != null) {
                        contentDao.updateFavorite(matched.id, true)
                        restoredFavCount++
                    }
                }
            }

            // 3. 퀴즈 로그 복원
            val logArray = root.optJSONArray("quizLogs")
            if (logArray != null) {
                val newLogs = mutableListOf<QuizLog>()
                for (i in 0 until logArray.length()) {
                    val obj = logArray.getJSONObject(i)
                    val itemId = obj.getLong("itemId")
                    val quizType = try {
                        QuizType.valueOf(obj.getString("quizType"))
                    } catch (e: Exception) {
                        QuizType.OX
                    }
                    val isCorrect = obj.getBoolean("isCorrect")
                    val answeredAt = obj.getLong("answeredAt")

                    newLogs.add(
                        QuizLog(
                            itemId = itemId,
                            quizType = quizType,
                            isCorrect = isCorrect,
                            answeredAt = answeredAt
                        )
                    )
                }
                if (newLogs.isNotEmpty()) {
                    quizLogDao.insertAll(newLogs)
                    restoredLogCount = newLogs.size
                }
            }

            RestoreResult(
                success = true,
                restoredCustomWords = restoredCustomCount,
                restoredFavorites = restoredFavCount,
                restoredLogs = restoredLogCount
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "백업 파일 복원 중 오류가 발생했습니다: ${e.localizedMessage}"
            )
        }
    }
}
