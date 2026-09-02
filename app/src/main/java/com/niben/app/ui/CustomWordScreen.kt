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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    ContentCategory.VOCAB to "단어",
    ContentCategory.KANJI to "한자",
    ContentCategory.SENTENCE to "문장",
    ContentCategory.HIRAGANA to "히라가나",
    ContentCategory.KATAKANA to "가타카나"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWordScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { NibenDatabase.getInstance(context).contentDao() }
    val ttsManager = rememberTtsManager()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: 나만의 단어, 1: 전체 검색
    val customItemList = remember { mutableStateListOf<ContentItem>() }
    val searchResultList = remember { mutableStateListOf<ContentItem>() }
    var searchQuery by remember { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }

    // 다이얼로그 상태
    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ContentItem?>(null) }
    var itemToDelete by remember { mutableStateOf<ContentItem?>(null) }

    LaunchedEffect(reloadKey, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            val items = dao.getCustomItems()
            customItemList.clear()
            customItemList.addAll(items)
        }
    }

    LaunchedEffect(searchQuery, selectedTabIndex) {
        if (selectedTabIndex == 1 && searchQuery.isNotBlank()) {
            val results = dao.searchItems(searchQuery)
            searchResultList.clear()
            searchResultList.addAll(results)
        } else if (selectedTabIndex == 1) {
            searchResultList.clear()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        editingItem = null
                        showEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 나만의 단어장",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = "직접 단어를 추가하고 관리하여 퀴즈에 출제합니다.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("등록한 단어 (${customItemList.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("전체 사전 검색") }
                    )
                }

                if (selectedTabIndex == 1) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("검색할 일본어, 발음, 한국어 뜻 입력") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                val currentList = if (selectedTabIndex == 0) customItemList else searchResultList

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTabIndex == 0) {
                                "등록된 나만의 단어가 없습니다.\n우측 하단의 '+' 버튼을 눌러 단어를 추가해보세요!"
                            } else if (searchQuery.isBlank()) {
                                "검색어를 입력하면 전체 단어를 조회할 수 있습니다."
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
                        items(currentList, key = { it.id }) { item ->
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CATEGORY_LABEL[item.category] ?: item.category.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (item.level != null) {
                                                Text(
                                                    text = " • N${6 - item.level}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (item.isCustom) {
                                                Text(
                                                    text = " • 사용자 등록",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // 발음 재생
                                            IconButton(onClick = { ttsManager.speak(item.japaneseText) }) {
                                                Text("🔊", fontSize = 16.sp)
                                            }

                                            // 즐겨찾기 토글
                                            IconButton(onClick = {
                                                val newFav = !item.isFavorite
                                                scope.launch {
                                                    dao.updateFavorite(item.id, newFav)
                                                    reloadKey++
                                                }
                                            }) {
                                                Text(
                                                    text = if (item.isFavorite) "★" else "☆",
                                                    fontSize = 18.sp,
                                                    color = if (item.isFavorite) Color(0xFFFFB300) else Color.Gray
                                                )
                                            }

                                            // 사용자 단어인 경우만 수정/삭제 허용
                                            if (item.isCustom) {
                                                IconButton(onClick = {
                                                    editingItem = item
                                                    showEditDialog = true
                                                }) {
                                                    Text("✏️", fontSize = 15.sp)
                                                }

                                                IconButton(onClick = {
                                                    itemToDelete = item
                                                }) {
                                                    Text("🗑️", fontSize = 15.sp)
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = item.japaneseText,
                                        fontSize = 22.sp,
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

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("홈으로")
                }
            }
        }
    }

    // 추가/수정 다이얼로그
    if (showEditDialog) {
        WordEditDialog(
            initialItem = editingItem,
            onDismiss = { showEditDialog = false },
            onSave = { savedItem ->
                scope.launch {
                    if (savedItem.id == 0L) {
                        dao.insert(savedItem)
                    } else {
                        dao.update(savedItem)
                    }
                    showEditDialog = false
                    reloadKey++
                }
            }
        )
    }

    // 삭제 확인 다이얼로그
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("단어 삭제") },
            text = { Text("'${item.japaneseText}' 단어를 정말 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            dao.deleteById(item.id)
                            itemToDelete = null
                            reloadKey++
                        }
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordEditDialog(
    initialItem: ContentItem?,
    onDismiss: () -> Unit,
    onSave: (ContentItem) -> Unit
) {
    var category by remember { mutableStateOf(initialItem?.category ?: ContentCategory.VOCAB) }
    var japaneseText by remember { mutableStateOf(initialItem?.japaneseText.orEmpty()) }
    var reading by remember { mutableStateOf(initialItem?.reading.orEmpty()) }
    var meaningKo by remember { mutableStateOf(initialItem?.meaningKo.orEmpty()) }
    var level by remember { mutableStateOf(initialItem?.level ?: 1) } // 1: N5, 5: N1
    var exampleSentence by remember { mutableStateOf(initialItem?.exampleSentence.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val isValid = japaneseText.isNotBlank() && meaningKo.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "새 단어 추가" else "단어 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 카테고리 선택
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = CATEGORY_LABEL[category] ?: category.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("카테고리") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        listOf(ContentCategory.VOCAB, ContentCategory.KANJI, ContentCategory.SENTENCE).forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(CATEGORY_LABEL[cat] ?: cat.name) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = japaneseText,
                    onValueChange = { japaneseText = it },
                    label = { Text("일본어 원문 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reading,
                    onValueChange = { reading = it },
                    label = { Text("요미가나(발음)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = meaningKo,
                    onValueChange = { meaningKo = it },
                    label = { Text("한국어 뜻 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("예문 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            ContentItem(
                                id = initialItem?.id ?: 0L,
                                category = category,
                                japaneseText = japaneseText.trim(),
                                reading = if (reading.isBlank()) japaneseText.trim() else reading.trim(),
                                meaningKo = meaningKo.trim(),
                                level = level,
                                exampleSentence = exampleSentence.trim().ifBlank { null },
                                source = "USER",
                                isFavorite = initialItem?.isFavorite ?: false,
                                isCustom = true
                            )
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
