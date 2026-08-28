package com.niben.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niben.app.data.LevelFilterStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val LEVEL_LABELS = mapOf(
    1 to "N5 (입문 - 가나 문자 및 최기초 어휘)",
    2 to "N4 (초급 - 일상 표현 및 한자 기초)",
    3 to "N3 (중급 - 회화 및 생활 한자)",
    4 to "N2 (준상급 - 실용적 독해 및 청해)",
    5 to "N1 (상급 - 고난도 어휘 및 전문 한자)"
)

@Composable
fun LevelFilterScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val levelsState = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(Unit) {
        val selected = LevelFilterStore.getSelectedLevelsFlow(context).first()
        for (level in 1..5) {
            levelsState[level] = selected.contains(level)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "학습 난이도 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )

            Text(
                text = "원하는 JLPT 난이도를 다중 선택할 수 있습니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 1~5레벨 리스팅
            for (level in 1..5) {
                val isChecked = levelsState[level] ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            val activeCount = levelsState.values.count { it }
                            if (!checked && activeCount <= 1) {
                                Toast.makeText(context, "최소 한 개 이상의 난이도를 선택해야 합니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                levelsState[level] = checked
                                scope.launch {
                                    LevelFilterStore.setLevelEnabled(context, level, checked)
                                }
                            }
                        }
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Level $level",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = LEVEL_LABELS[level].orEmpty(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
            ) {
                Text("설정 완료 (홈으로)")
            }
        }
    }
}
