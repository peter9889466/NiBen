package com.niben.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.niben.app.notification.QuizNotifier
import kotlin.random.Random

/**
 * 주기적으로 잠금화면 퀴즈 알림을 새 문제로 교체한다.
 * 매번 OX(즉시 응답)와 3/4지선다(탭해서 앱 화면에서 응답) 중 하나를 무작위로 고른다.
 */
class QuizRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Random.nextInt(100) < OX_WEIGHT_PERCENT) {
            QuizNotifier.showNextQuiz(applicationContext)
        } else {
            val choiceCount = if (Random.nextBoolean()) 3 else 4
            QuizNotifier.showMultipleChoicePrompt(applicationContext, choiceCount)
        }
        return Result.success()
    }

    private companion object {
        const val OX_WEIGHT_PERCENT = 65
    }
}
