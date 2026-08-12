package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.editor.components.SearchReplaceBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    // 导出文件 Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportToUri(context, it) }
    }

    // 拦截返回键：自动保存并返回
    BackHandler {
        viewModel.saveNoteNow()
        onBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // 实时字数统计
                    Text(
                        text = "${uiState.wordCount} 字 / ${uiState.charCount} 字符",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveNoteNow()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回并保存"
                        )
                    }
                },
                actions = {
                    // 撤销 (Undo)
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销",
                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    // 重做 (Redo)
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "重做",
                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    // 锁定/解锁切换
                    IconButton(
                        onClick = { viewModel.toggleLock() },
                        modifier = Modifier.testTag("editor_lock_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (uiState.isLocked) "当前锁定（点击解锁编辑）" else "当前编辑状态（点击锁定）",
                            tint = if (uiState.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    // 更多功能菜单
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "更多功能")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (uiState.isPinned) "取消置顶" else "置顶笔记") },
                            onClick = {
                                showMenu = false
                                viewModel.togglePin()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("搜索与替换") },
                            onClick = {
                                showMenu = false
                                viewModel.toggleSearchReplace()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("一键排版格式化") },
                            onClick = {
                                showMenu = false
                                viewModel.formatContent()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("导出到文件 (.txt / .md)") },
                            onClick = {
                                showMenu = false
                                val exportFileName = (uiState.title.ifBlank { "笔记导出" }) + ".txt"
                                exportLauncher.launch(exportFileName)
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索/替换栏
            AnimatedVisibility(
                visible = uiState.isSearchReplaceVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchReplaceBar(
                    searchQuery = uiState.searchQuery,
                    replaceQuery = uiState.replaceQuery,
                    onSearchChange = { viewModel.onSearchQueryChanged(it) },
                    onReplaceChange = { viewModel.onReplaceQueryChanged(it) },
                    onReplaceOne = { viewModel.replaceOne() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onClose = { viewModel.toggleSearchReplace() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 标题输入框
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    placeholder = {
                        Text(
                            text = if (uiState.isLocked) "只读状态（点击上方锁图标解除锁定）" else "输入笔记标题...",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    },
                    readOnly = uiState.isLocked,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 内容编辑区
                TextField(
                    value = uiState.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    placeholder = {
                        Text(
                            text = if (uiState.isLocked) "当前处于锁定状态" else "写点什么吧...",
                            style = TextStyle(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    },
                    readOnly = uiState.isLocked,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("note_content_input")
                )
            }
        }
    }
}
