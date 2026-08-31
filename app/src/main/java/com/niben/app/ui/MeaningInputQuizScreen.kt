package com.niben.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niben.app.data.NibenDatabase
import com.niben.app.data.QuizLog
import com.niben.app.data.QuizType
import com.niben.app.data.RecentItemsStore
import com.niben.app.quiz.MeaningInputQuiz
import com.niben.app.quiz.MeaningValidator
import com.niben.app.quiz.QuizGenerator
import kotlinx.coroutines.launch

@Composable
fun MeaningInputQuizScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val dao = remember { NibenDatabase.getInstance(context).contentDao() }
    val quizLogDao = remember { NibenDatabase.getInstance(context).quizLogDao() }

    var quiz by remember { mutableStateOf<MeaningInputQuiz?>(null) }
    var userInput by remember { mutableStateOf("") }
    var isAnswered by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(reloadKey) {
        loading = true
        userInput = ""
        isAnswered = false
        isCorrect = false
        val excludeIds = RecentItemsStore.getRecentIds(context)
        val next = QuizGenerator.generateMeaningInput(dao, excludeIds, context)
        if (next != null) {
            RecentItemsStore.recordShown(context, next.itemId)
        }
        quiz = next
        loading = false
    }

    fun submitAnswer() {
        val currentQuiz = quiz ?: return
        if (isAnswered || userInput.isBlank()) return

        keyboardController?.hide()
        isAnswered = true
        isCorrect = MeaningValidator.isCorrect(
            userInput = userInput,
            meaningKo = currentQuiz.meaningKo,
            reading = currentQuiz.reading,
            category = currentQuiz.category
        )

        scope.launch {
            quizLogDao.insert(
                QuizLog(
                    itemId = currentQuiz.itemId,
                    quizType = QuizType.MEANING_INPUT,
                    isCorrect = isCorrect,
                    answeredAt = System.currentTimeMillis()
                )
            )
        }
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
                    text = "주관식 뜻 입력 퀴즈",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = currentQuiz.questionText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (currentQuiz.reading.isNotEmpty() && currentQuiz.reading != currentQuiz.japaneseText) {
                    Text(
                        text = "발음: ${currentQuiz.reading}",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = userInput,
                    onValueChange = { if (!isAnswered) userInput = it },
                    label = { Text("한국어 뜻(또는 발음) 입력") },
                    singleLine = true,
                    enabled = !isAnswered,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isAnswered) {
                    Button(
                        onClick = { submitAnswer() },
                        enabled = userInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("정답 확인")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isCorrect) "정답입니다!" else "오답입니다",
                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "정답: ${currentQuiz.meaningKo}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (currentQuiz.reading.isNotEmpty()) {
                                Text(
                                    text = "읽기: ${currentQuiz.reading}",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { reloadKey++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다음 문제")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("홈으로")
        }
    }
}
