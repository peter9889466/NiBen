package com.niben.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.niben.app.data.QuizType
import com.niben.app.data.RecentItemsStore
import com.niben.app.quiz.QuizGenerator
import com.niben.app.quiz.SentenceCompletionQuiz
import kotlinx.coroutines.launch

@Composable
fun SentenceCompletionQuizScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { NibenDatabase.getInstance(context).contentDao() }
    val quizLogDao = remember { NibenDatabase.getInstance(context).quizLogDao() }

    var quiz by remember { mutableStateOf<SentenceCompletionQuiz?>(null) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(reloadKey) {
        loading = true
        selectedIndex = -1
        val excludeIds = RecentItemsStore.getRecentIds(context)
        val next = QuizGenerator.generateSentenceCompletion(dao, excludeIds, context)
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
            quiz == null -> Text("출제할 예문이 부족해요. 콘텐츠를 더 추가해 주세요.")
            else -> {
                val currentQuiz = quiz!!

                Text(
                    text = "예문 완성형 퀴즈",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "빈칸에 들어갈 알맞은 단어는?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentQuiz.sentenceWithBlank,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = currentQuiz.koreanMeaning,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                currentQuiz.options.forEachIndexed { index, option ->
                    val isAnswered = selectedIndex != -1
                    val isCorrectOption = index == currentQuiz.correctIndex
                    val isSelected = index == selectedIndex

                    val backgroundColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                        isCorrectOption -> Color(0xFFA5D6A7)
                        isSelected -> Color(0xFFEF9A9A)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Text(
                        text = "${index + 1}. $option",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .background(backgroundColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isAnswered) {
                                selectedIndex = index
                                val isCorrect = index == currentQuiz.correctIndex
                                scope.launch {
                                    quizLogDao.insert(
                                        QuizLog(
                                            itemId = currentQuiz.itemId,
                                            quizType = QuizType.SENTENCE_COMPLETION,
                                            isCorrect = isCorrect,
                                            answeredAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            .padding(14.dp),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (selectedIndex != -1) {
                    val isCorrect = selectedIndex == currentQuiz.correctIndex
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isCorrect) "정답입니다!" else "오답입니다",
                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "완성 문장: ${currentQuiz.fullSentence}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentQuiz.fullReading.isNotEmpty() && currentQuiz.fullReading != currentQuiz.fullSentence) {
                                Text(
                                    text = "읽기: ${currentQuiz.fullReading}",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { reloadKey++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다음 문제")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("홈으로")
        }
    }
}
