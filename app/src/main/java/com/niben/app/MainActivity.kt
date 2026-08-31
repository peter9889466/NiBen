package com.niben.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.niben.app.notification.QuizNotifier
import com.niben.app.ui.CategoryRatioScreen
import com.niben.app.ui.IncorrectNoteScreen
import com.niben.app.ui.LevelFilterScreen
import com.niben.app.ui.MeaningInputQuizScreen
import com.niben.app.ui.QuizScreen
import com.niben.app.ui.SentenceCompletionQuizScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openQuizChoiceCount by mutableStateOf<Int?>(null)
    private var showCategoryRatioScreen by mutableStateOf(false)
    private var showLevelFilterScreen by mutableStateOf(false)
    private var showIncorrectNoteScreen by mutableStateOf(false)
    private var showMeaningInputScreen by mutableStateOf(false)
    private var showSentenceCompletionScreen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openQuizChoiceCount = choiceCountFromIntent(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val choiceCount = openQuizChoiceCount
                    when {
                        choiceCount != null ->
                            QuizScreen(choiceCount = choiceCount, onExit = { openQuizChoiceCount = null })
                        showMeaningInputScreen ->
                            MeaningInputQuizScreen(onExit = { showMeaningInputScreen = false })
                        showSentenceCompletionScreen ->
                            SentenceCompletionQuizScreen(onExit = { showSentenceCompletionScreen = false })
                        showCategoryRatioScreen ->
                            CategoryRatioScreen(onExit = { showCategoryRatioScreen = false })
                        showLevelFilterScreen ->
                            LevelFilterScreen(onExit = { showLevelFilterScreen = false })
                        showIncorrectNoteScreen ->
                            IncorrectNoteScreen(onExit = { showIncorrectNoteScreen = false })
                        else ->
                            HelloNiBen(
                                onOpenMultipleChoiceQuiz = { openQuizChoiceCount = 4 },
                                onOpenMeaningInputQuiz = { showMeaningInputScreen = true },
                                onOpenSentenceCompletionQuiz = { showSentenceCompletionScreen = true },
                                onOpenCategoryRatio = { showCategoryRatioScreen = true },
                                onOpenLevelFilter = { showLevelFilterScreen = true },
                                onOpenIncorrectNote = { showIncorrectNoteScreen = true }
                            )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openQuizChoiceCount = choiceCountFromIntent(intent)
    }

    private fun choiceCountFromIntent(intent: Intent?): Int? {
        if (intent?.getBooleanExtra(QuizNotifier.EXTRA_OPEN_QUIZ, false) != true) return null
        return intent.getIntExtra(QuizNotifier.EXTRA_QUIZ_CHOICE_COUNT, 4)
    }
}

@Composable
fun HelloNiBen(
    onOpenMultipleChoiceQuiz: () -> Unit = {},
    onOpenMeaningInputQuiz: () -> Unit = {},
    onOpenSentenceCompletionQuiz: () -> Unit = {},
    onOpenCategoryRatio: () -> Unit = {},
    onOpenLevelFilter: () -> Unit = {},
    onOpenIncorrectNote: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 거부해도 앱 자체는 계속 사용 가능, 알림만 표시되지 않음 */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 32.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "こんにちは", fontSize = 36.sp)
        Text(text = "NiBen — 오늘의 일본어 퀴즈", fontSize = 16.sp)

        Button(
            onClick = { scope.launch { QuizNotifier.showNextQuiz(context) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("OX 문제 알림 보내기")
        }
        Button(
            onClick = onOpenMultipleChoiceQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("선택형 퀴즈 화면 열기")
        }
        Button(
            onClick = onOpenMeaningInputQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("뜻 입력형 주관식 퀴즈")
        }
        Button(
            onClick = onOpenSentenceCompletionQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("예문 완성형 퀴즈")
        }
        Button(
            onClick = onOpenCategoryRatio,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("카테고리 비율 설정")
        }
        Button(
            onClick = onOpenLevelFilter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("학습 난이도 설정")
        }
        Button(
            onClick = onOpenIncorrectNote,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("오답노트 복습")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HelloNiBenPreview() {
    MaterialTheme {
        HelloNiBen()
    }
}
