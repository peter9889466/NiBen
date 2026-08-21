package com.niben.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niben.app.data.NibenDatabase
import com.niben.app.data.QuizLog
import com.niben.app.data.QuizType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuizActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(QuizNotifier.EXTRA_ITEM_ID, -1L)
        val question = intent.getStringExtra(QuizNotifier.EXTRA_QUESTION).orEmpty()
        val correctAnswer = intent.getBooleanExtra(QuizNotifier.EXTRA_CORRECT_ANSWER, true)
        val answer = intent.getBooleanExtra(QuizNotifier.EXTRA_ANSWER, false)
        val isCorrect = answer == correctAnswer

        val resultText = if (isCorrect) {
            "정답입니다! - $question"
        } else {
            val correctLabel = if (correctAnswer) "O" else "X"
            "오답입니다 (정답: $correctLabel) - $question"
        }

        val builder = NotificationCompat.Builder(context, QuizNotifier.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("오늘의 일본어 퀴즈")
            .setContentText(resultText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)

        NotificationManagerCompat.from(context).notify(QuizNotifier.NOTIFICATION_ID, builder.build())

        if (itemId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = NibenDatabase.getInstance(context).quizLogDao()
                dao.insert(
                    QuizLog(
                        itemId = itemId,
                        quizType = QuizType.OX,
                        isCorrect = isCorrect,
                        answeredAt = System.currentTimeMillis()
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
