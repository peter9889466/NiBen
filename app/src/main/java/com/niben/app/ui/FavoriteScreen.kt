package com.niben.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.niben.app.data.ContentCategory
import com.niben.app.data.ContentItem
import com.niben.app.data.NibenDatabase
import com.niben.app.util.rememberTtsManager
import kotlinx.coroutines.launch

private val CATEGORY_LABEL = mapOf(
    ContentCategory.HIRAGANA to "히라가나",
    ContentCategory.KATAKANA to "가타카나",
    ContentCategory.KANJI to "한자",
    ContentCategory.VOCAB to "단어",
    ContentCategory.SENTENCE to "문장"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onExit: () -> Unit,
    onStartQuiz: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { NibenDatabase.getInstance(context).contentDao() }
    val ttsManager = rememberTtsManager()

    val favoriteList = remember { mutableStateListOf<ContentItem>() }
    var selectedCategory by remember { mutableStateOf<ContentCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        val items = dao.getFavorites()
        favoriteList.clear()
        favoriteList.addAll(items)
    }

    val filteredList = favoriteList.filter { item ->
        val matchesCategory = (selectedCategory == null || item.category == selectedCategory)
        val matchesQuery = searchQuery.isBlank() ||
                item.japaneseText.contains(searchQuery, ignoreCase = true) ||
                item.reading.contains(searchQuery, ignoreCase = true) ||
                item.meaningKo.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 타이틀
            Text(
                text = "⭐ 즐겨찾기 단어장",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            Text(
                text = "자주 복습하고 싶은 단어와 문장 모음",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 검색창
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("단어, 발음, 뜻 검색") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // 카테고리 필터 칩
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("전체 (${favoriteList.size})") }
                    )
                }
                items(ContentCategory.entries.toTypedArray()) { cat ->
                    val count = favoriteList.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text("${CATEGORY_LABEL[cat]} ($count)") }
                    )
                }
            }

            // 즐겨찾기 목록
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (favoriteList.isEmpty()) {
                            "즐겨찾기한 단어가 없습니다.\n퀴즈나 단어장에서 별표(★)를 눌러 추가해보세요!"
                        } else {
                            "검색 결과가 없습니다."
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = CATEGORY_LABEL[item.category].orEmpty(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 발음 듣기 버튼
                                        IconButton(onClick = { ttsManager.speak(item.japaneseText) }) {
                                            Text("🔊", fontSize = 18.sp)
                                        }

                                        // 즐겨찾기 토글 버튼
                                        IconButton(onClick = {
                                            scope.launch {
                                                dao.updateFavorite(item.id, false)
                                                favoriteList.remove(item)
                                            }
                                        }) {
                                            Text("★", fontSize = 20.sp, color = Color(0xFFFFB300))
                                        }
                                    }
                                }

                                Text(
                                    text = item.japaneseText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                if (item.reading.isNotEmpty() && item.reading != item.japaneseText) {
                                    Text(
                                        text = "발음: ${item.reading}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Text(
                                    text = "뜻: ${item.meaningKo}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                if (!item.exampleSentence.isNullOrBlank()) {
                                    Text(
                                        text = "예문: ${item.exampleSentence}",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("홈으로")
                }
            }
        }
    }
}
