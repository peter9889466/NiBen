package com.niben.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.niben.app.data.BackupManager
import com.niben.app.data.BackupSummary
import com.niben.app.data.NibenDatabase
import com.niben.app.data.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { NibenDatabase.getInstance(context) }
    val contentDao = db.contentDao()
    val quizLogDao = db.quizLogDao()

    var customWordCount by remember { mutableIntStateOf(0) }
    var favoriteCount by remember { mutableIntStateOf(0) }
    var quizLogCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // 복원 확인 다이얼로그용 상태
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }

    val scrollState = rememberScrollState()

    fun refreshStats() {
        scope.launch {
            customWordCount = contentDao.getCustomItems().size
            favoriteCount = contentDao.getFavoriteCount()
            quizLogCount = quizLogDao.getTotalSolvedCount()
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    // 내보내기(Export) 파일 생성 런처
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val json = withContext(Dispatchers.IO) {
                        BackupManager.exportToJson(contentDao, quizLogDao)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "백업 파일이 성공적으로 저장되었습니다!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "백업 저장 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // 가져오기(Import) 파일 선택 런처
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                        }
                    }
                    if (!json.isNullOrBlank()) {
                        val summary = BackupManager.parseSummary(json)
                        if (summary != null) {
                            pendingRestoreJson = json
                            pendingSummary = summary
                        } else {
                            Toast.makeText(context, "유효한 NiBen 백업 파일이 아닙니다.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "파일을 읽는 중 오류가 발생했습니다: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💾 데이터 백업 및 복원",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            Text(
                text = "서버 연결 없이 내 기기에 안전하게 JSON 파일로 백업하고 복원합니다.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 1. 현재 데이터 현황 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 현재 기기의 학습 데이터",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• 직접 등록한 단어:", fontSize = 14.sp)
                        Text("${customWordCount}개", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• 즐겨찾기 단어:", fontSize = 14.sp)
                        Text("${favoriteCount}개", fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• 퀴즈 풀이 이력 로그:", fontSize = 14.sp)
                        Text("${quizLogCount}건", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 백업 파일 내보내기 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📤 백업 파일 내보내기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "등록한 단어, 즐겨찾기, 퀴즈 로그를 JSON 파일로 다운로드 폴더 등에 저장합니다.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("niben_backup_$timeStamp.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("JSON 파일로 내보내기")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 백업 파일 가져오기 및 복원 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📥 백업 파일 가져와 복원하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "기존에 저장한 NiBen 백업 JSON 파일을 선택해 학습 데이터를 현재 기기에 병합 복원합니다.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("백업 파일 선택하기")
                    }
                }
            }

            // 복원 결과 표시
            restoreResult?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (res.success) "✅ 복원 완료!" else "❌ 복원 실패",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        if (res.success) {
                            Text(
                                text = "• 등록 단어: ${res.restoredCustomWords}개 복원\n• 즐겨찾기: ${res.restoredFavorites}개 반영\n• 퀴즈 로그: ${res.restoredLogs}건 복원",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = res.errorMessage ?: "오류가 발생했습니다.",
                                fontSize = 13.sp,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("홈으로")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 복원 확인 다이얼로그
    pendingSummary?.let { summary ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(summary.exportedAt))
        AlertDialog(
            onDismissRequest = {
                pendingSummary = null
                pendingRestoreJson = null
            },
            title = { Text("백업 데이터 복원 확인") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("선택한 백업 파일의 정보입니다:")
                    Text("• 백업 시각: $dateStr", fontSize = 13.sp)
                    Text("• 사용자 단어: ${summary.customWordCount}개", fontSize = 13.sp)
                    Text("• 즐겨찾기: ${summary.favoriteCount}개", fontSize = 13.sp)
                    Text("• 퀴즈 풀이 이력: ${summary.quizLogCount}건", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("이 데이터를 현재 기기에 병합하여 복원하시겠습니까?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson
                        pendingSummary = null
                        pendingRestoreJson = null
                        if (json != null) {
                            scope.launch {
                                isLoading = true
                                val result = withContext(Dispatchers.IO) {
                                    BackupManager.importFromJson(json, contentDao, quizLogDao)
                                }
                                restoreResult = result
                                refreshStats()
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text("복원 진행")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSummary = null
                        pendingRestoreJson = null
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}
