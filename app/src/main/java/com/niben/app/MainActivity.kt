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
import com.niben.app.notification.QuizNotifier
import com.niben.app.ui.QuizScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openQuizChoiceCount by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openQuizChoiceCount = choiceCountFromIntent(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val choiceCount = openQuizChoiceCount
                    if (choiceCount != null) {
                        QuizScreen(choiceCount = choiceCount, onExit = { openQuizChoiceCount = null })
                    } else {
                        HelloNiBen(onOpenMultipleChoiceQuiz = { openQuizChoiceCount = 4 })
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
fun HelloNiBen(onOpenMultipleChoiceQuiz: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "こんにちは", fontSize = 36.sp)
        Text(text = "NiBen — 오늘의 일본어 퀴즈", fontSize = 16.sp)
        Button(
            onClick = { scope.launch { QuizNotifier.showNextQuiz(context) } },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("OX 문제 알림 보내기")
        }
        Button(
            onClick = onOpenMultipleChoiceQuiz,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("선택형 퀴즈 화면 열기")
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
