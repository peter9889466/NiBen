package com.niben.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.niben.app.data.CategoryRatioStore
import com.niben.app.data.ContentCategory
import kotlinx.coroutines.launch

private val CATEGORY_LABEL = mapOf(
    ContentCategory.HIRAGANA to "히라가나",
    ContentCategory.KATAKANA to "가타카나",
    ContentCategory.KANJI to "한자",
    ContentCategory.VOCAB to "단어",
    ContentCategory.SENTENCE to "문장"
)

private const val STEP = 5

/**
 * 카테고리별 문제 출제 비율(가중치)을 조절하는 화면. 가중치가 클수록 그 카테고리가
 * 더 자주 출제된다. 값은 CategoryRatioStore(DataStore)에 즉시 저장된다.
 */
@Composable
fun CategoryRatioScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weights = remember { mutableStateMapOf<ContentCategory, Int>() }

    LaunchedEffect(Unit) {
        CategoryRatioStore.getWeights(context).forEach { (category, weight) ->
            weights[category] = weight
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "카테고리별 출제 비율")
        Text(text = "값이 클수록 더 자주 출제돼요", modifier = Modifier.padding(bottom = 16.dp))

        ContentCategory.entries.forEach { category ->
            val weight = weights[category] ?: CategoryRatioStore.DEFAULT_WEIGHT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = CATEGORY_LABEL[category].orEmpty(), modifier = Modifier.width(80.dp))
                IconButton(onClick = {
                    val next = (weight - STEP).coerceAtLeast(CategoryRatioStore.MIN_WEIGHT)
                    weights[category] = next
                    scope.launch { CategoryRatioStore.setWeight(context, category, next) }
                }) { Text("-") }
                Text(text = weight.toString(), modifier = Modifier.width(32.dp))
                IconButton(onClick = {
                    val next = (weight + STEP).coerceAtMost(CategoryRatioStore.MAX_WEIGHT)
                    weights[category] = next
                    scope.launch { CategoryRatioStore.setWeight(context, category, next) }
                }) { Text("+") }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    ContentCategory.entries.forEach { category ->
                        CategoryRatioStore.setWeight(context, category, CategoryRatioStore.DEFAULT_WEIGHT)
                        weights[category] = CategoryRatioStore.DEFAULT_WEIGHT
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("기본값으로 초기화")
        }

        OutlinedButton(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) {
            Text("홈으로")
        }
    }
}
