package com.niben.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niben.app.MainActivity
import com.niben.app.data.NibenDatabase
import com.niben.app.data.RecentItemsStore
import com.niben.app.quiz.OxQuiz
import com.niben.app.quiz.QuizGenerator

/**
 * 잠금화면에 퀴즈 알림을 띄운다.
 * - OX: 알림 액션 버튼으로 그 자리에서 즉시 응답(QuizActionReceiver가 채점).
 * - 3/4지선다: 알림을 탭하면 앱이 열리며 선택형 퀴즈 화면으로 진입(QuizScreen).
 */
object QuizNotifier {
    const val CHANNEL_ID = "niben_quiz_channel"
    const val NOTIFICATION_ID = 1001

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_QUESTION = "extra_question"
    const val EXTRA_CORRECT_ANSWER = "extra_correct_answer"
    const val EXTRA_ANSWER = "extra_answer"

    /** MainActivity가 이 값을 읽으면 홈 대신 선택형 퀴즈 화면을 바로 연다. */
    const val EXTRA_OPEN_QUIZ = "extra_open_quiz"
    const val EXTRA_QUIZ_CHOICE_COUNT = "extra_quiz_choice_count"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "일본어 퀴즈",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "잠금화면에 노출되는 오늘의 일본어 퀴즈"
            setSound(null, null)
            enableVibration(false)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /** content_item에서 무작위 OX 문제를 뽑아 알림으로 표시한다(최근 출제 항목은 제외). */
    suspend fun showNextQuiz(context: Context) {
        val dao = NibenDatabase.getInstance(context).contentDao()
        val excludeIds = RecentItemsStore.getRecentIds(context)
        val quiz = QuizGenerator.generateOx(dao, excludeIds, context) ?: return
        RecentItemsStore.recordShown(context, quiz.itemId)
        showQuiz(context, quiz)
    }

    /** 알림 탭 시 앱의 선택형 퀴즈 화면(QuizScreen)으로 바로 진입하도록 안내 알림만 띄운다. */
    fun showMultipleChoicePrompt(context: Context, choiceCount: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_QUIZ, true)
            putExtra(EXTRA_QUIZ_CHOICE_COUNT, choiceCount)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = if (choiceCount >= 4) "4지선다" else "3지선다"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("오늘의 일본어 퀴즈")
            .setContentText("$label 퀴즈가 준비됐어요 - 탭해서 풀기")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun showQuiz(context: Context, quiz: OxQuiz) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("오늘의 일본어 퀴즈")
            .setContentText(quiz.questionText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(buildAction(context, quiz, answer = true, label = "O"))
            .addAction(buildAction(context, quiz, answer = false, label = "X"))

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun buildAction(
        context: Context,
        quiz: OxQuiz,
        answer: Boolean,
        label: String
    ): NotificationCompat.Action {
        val intent = Intent(context, QuizActionReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_ID, quiz.itemId)
            putExtra(EXTRA_QUESTION, quiz.questionText)
            putExtra(EXTRA_CORRECT_ANSWER, quiz.correctAnswer)
            putExtra(EXTRA_ANSWER, answer)
        }
        val requestCode = if (answer) 1 else 2
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }
}
