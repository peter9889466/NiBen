package com.niben.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niben.app.data.NibenDatabase
import com.niben.app.data.QuizLog
import com.niben.app.data.RecentItemsStore
import com.niben.app.quiz.MultipleChoiceQuiz
import com.niben.app.quiz.QuizGenerator
import kotlinx.coroutines.launch

/**
 * 알림 탭으로 진입하는 3/4지선다 앱 내 퀴즈 화면.
 * 선택 즉시 정답 여부를 색상으로 피드백하고, quiz_log에 기록한다.
 */
@Composable
fun QuizScreen(choiceCount: Int, onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { NibenDatabase.getInstance(context).contentDao() }
    val quizLogDao = remember { NibenDatabase.getInstance(context).quizLogDao() }

    var quiz by remember { mutableStateOf<MultipleChoiceQuiz?>(null) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(reloadKey) {
        loading = true
        selectedIndex = -1
        val excludeIds = RecentItemsStore.getRecentIds(context)
        val next = QuizGenerator.generateMultipleChoice(dao, choiceCount, excludeIds, context)
        if (next != null) {
            RecentItemsStore.recordShown(context, next.itemId)
        }
        quiz = next
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            loading -> Text("문제를 불러오는 중...")
            quiz == null -> Text("출제할 문제가 부족해요. 콘텐츠를 더 추가해 주세요.")
            else -> {
                val currentQuiz = quiz!!
                Text(
                    text = currentQuiz.questionText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                currentQuiz.options.forEachIndexed { index, option ->
                    QuizOptionRow(
                        text = option,
                        isSelected = index == selectedIndex,
                        isCorrectOption = index == currentQuiz.correctIndex,
                        isAnswered = selectedIndex != -1,
                        onClick = {
                            if (selectedIndex == -1) {
                                selectedIndex = index
                                val isCorrect = index == currentQuiz.correctIndex
                                scope.launch {
                                    quizLogDao.insert(
                                        QuizLog(
                                            itemId = currentQuiz.itemId,
                                            quizType = currentQuiz.quizType,
                                            isCorrect = isCorrect,
                                            answeredAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                if (selectedIndex != -1) {
                    val isCorrect = selectedIndex == currentQuiz.correctIndex
                    Text(
                        text = if (isCorrect) "정답입니다!" else "오답입니다",
                        color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Button(
                        onClick = { reloadKey++ },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("다음 문제")
                    }
                }
            }
        }

        OutlinedButton(onClick = onExit, modifier = Modifier.padding(top = 24.dp)) {
            Text("홈으로")
        }
    }
}

@Composable
private fun QuizOptionRow(
    text: String,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
        isCorrectOption -> Color(0xFFA5D6A7)
        isSelected -> Color(0xFFEF9A9A)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isAnswered, onClick = onClick)
            .padding(16.dp),
        fontSize = 18.sp
    )
}
