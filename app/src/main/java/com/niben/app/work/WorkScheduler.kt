package com.niben.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 문제 교체 주기 작업을 등록한다. WorkManager는 자체 BOOT_COMPLETED 리시버로
 * 등록된 주기 작업을 재부팅 후에도 자동 유지하므로 별도 처리가 필요 없다.
 * 앱이 실행될 때마다(NibenApplication.onCreate) KEEP 정책으로 재등록해도
 * 이미 예약된 작업은 중복 생성되지 않는다.
 */
object WorkScheduler {
    private const val UNIQUE_WORK_NAME = "quiz_refresh_work"
    private const val REFRESH_INTERVAL_HOURS = 3L

    fun scheduleQuizRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<QuizRefreshWorker>(
            REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
