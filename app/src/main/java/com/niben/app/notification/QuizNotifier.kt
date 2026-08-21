package com.niben.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niben.app.data.NibenDatabase
import com.niben.app.quiz.OxQuiz
import com.niben.app.quiz.QuizGenerator

/**
 * 잠금화면에 OX 퀴즈 알림을 띄우고, 액션 버튼 클릭 시 앱을 열지 않고도
 * QuizActionReceiver가 정답을 판정해 알림 텍스트를 갱신하도록 한다.
 */
object QuizNotifier {
    const val CHANNEL_ID = "niben_quiz_channel"
    const val NOTIFICATION_ID = 1001

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_QUESTION = "extra_question"
    const val EXTRA_CORRECT_ANSWER = "extra_correct_answer"
    const val EXTRA_ANSWER = "extra_answer"

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

    /** content_item에서 무작위 OX 문제를 뽑아 알림으로 표시한다. */
    suspend fun showNextQuiz(context: Context) {
        val dao = NibenDatabase.getInstance(context).contentDao()
        val quiz = QuizGenerator.generateOx(dao) ?: return
        showQuiz(context, quiz)
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
