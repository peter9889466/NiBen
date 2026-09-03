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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.niben.app.notification.QuizNotifier
import com.niben.app.ui.BackupRestoreScreen
import com.niben.app.ui.CategoryRatioScreen
import com.niben.app.ui.CustomWordScreen
import com.niben.app.ui.FavoriteScreen
import com.niben.app.ui.IncorrectNoteScreen
import com.niben.app.ui.LevelFilterScreen
import com.niben.app.ui.MeaningInputQuizScreen
import com.niben.app.ui.QuizScreen
import com.niben.app.ui.SentenceCompletionQuizScreen
import com.niben.app.ui.StatisticsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openQuizChoiceCount by mutableStateOf<Int?>(null)
    private var showMeaningInputScreen by mutableStateOf(false)
    private var showSentenceCompletionScreen by mutableStateOf(false)
    private var showFavoriteScreen by mutableStateOf(false)
    private var showCustomWordScreen by mutableStateOf(false)
    private var showIncorrectNoteScreen by mutableStateOf(false)
    private var showStatisticsScreen by mutableStateOf(false)
    private var showCategoryRatioScreen by mutableStateOf(false)
    private var showLevelFilterScreen by mutableStateOf(false)
    private var showBackupRestoreScreen by mutableStateOf(false)

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
                        showFavoriteScreen ->
                            FavoriteScreen(onExit = { showFavoriteScreen = false })
                        showCustomWordScreen ->
                            CustomWordScreen(onExit = { showCustomWordScreen = false })
                        showIncorrectNoteScreen ->
                            IncorrectNoteScreen(onExit = { showIncorrectNoteScreen = false })
                        showStatisticsScreen ->
                            StatisticsScreen(onExit = { showStatisticsScreen = false })
                        showCategoryRatioScreen ->
                            CategoryRatioScreen(onExit = { showCategoryRatioScreen = false })
                        showLevelFilterScreen ->
                            LevelFilterScreen(onExit = { showLevelFilterScreen = false })
                        showBackupRestoreScreen ->
                            BackupRestoreScreen(onExit = { showBackupRestoreScreen = false })
                        else ->
                            HelloNiBen(
                                onOpenMultipleChoiceQuiz = { openQuizChoiceCount = 4 },
                                onOpenMeaningInputQuiz = { showMeaningInputScreen = true },
                                onOpenSentenceCompletionQuiz = { showSentenceCompletionScreen = true },
                                onOpenFavorite = { showFavoriteScreen = true },
                                onOpenCustomWord = { showCustomWordScreen = true },
                                onOpenIncorrectNote = { showIncorrectNoteScreen = true },
                                onOpenStatistics = { showStatisticsScreen = true },
                                onOpenCategoryRatio = { showCategoryRatioScreen = true },
                                onOpenLevelFilter = { showLevelFilterScreen = true },
                                onOpenBackupRestore = { showBackupRestoreScreen = true }
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
    onOpenFavorite: () -> Unit = {},
    onOpenCustomWord: () -> Unit = {},
    onOpenIncorrectNote: () -> Unit = {},
    onOpenStatistics: () -> Unit = {},
    onOpenCategoryRatio: () -> Unit = {},
    onOpenLevelFilter: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {}
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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 헤더
        Text(
            text = "こんにちは",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "NiBen — 오늘의 일본어 학습",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 섹션 1: 퀴즈로 학습하기
        SectionCard(title = "📝 퀴즈로 학습하기") {
            Button(
                onClick = { scope.launch { QuizNotifier.showNextQuiz(context) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔔 잠금화면 OX 알림 보내기")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenMultipleChoiceQuiz,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎯 4지선다 객관식 퀴즈")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenMeaningInputQuiz,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✍️ 주관식 뜻 입력 퀴즈")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenSentenceCompletionQuiz,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💬 예문 완성형 빈칸 퀴즈")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 섹션 2: 나만의 학습 & 복습
        SectionCard(title = "⭐ 나만의 학습 & 복습") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenFavorite,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("⭐ 즐겨찾기")
                }

                Button(
                    onClick = onOpenCustomWord,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("📖 단어 관리")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenIncorrectNote,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("❌ 오답노트 집중 복습")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 섹션 3: 통계 및 맞춤 설정
        SectionCard(title = "⚙️ 통계 및 맞춤 설정") {
            Button(
                onClick = onOpenStatistics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text("📊 학습 통계 대시보드")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCategoryRatio,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("카테고리 비율")
                }

                OutlinedButton(
                    onClick = onOpenLevelFilter,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("난이도 설정")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenBackupRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 데이터 백업 및 복원")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
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

