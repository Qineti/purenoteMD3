package com.example.ui.list

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.list.components.CreateFolderDialog
import com.example.ui.list.components.FolderDropdownSelector
import com.example.ui.list.components.FolderManagerDialog
import com.example.ui.list.components.NoteItem
import com.example.ui.list.components.NoteSearchBar
import com.example.ui.list.components.SortDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToRecycleBin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showTopMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showFolderManagerDialog by remember { mutableStateOf(false) }
    var showBatchMoveFolderMenu by remember { mutableStateOf(false) }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 导入文档 Picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importNoteFromUri(context, it) }
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
            if (uiState.isMultiSelectMode) {
                // 多选模式 TopBar
                TopAppBar(
                    title = {
                        Text(
                            text = "已选择 ${uiState.selectedNoteIds.size} 项",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.selectAllNotes(uiState.notes.map { it.id })
                        }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "全选")
                        }

                        Box {
                            IconButton(onClick = { showBatchMoveFolderMenu = true }) {
                                Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = "移动至分组")
                            }

                            DropdownMenu(
                                expanded = showBatchMoveFolderMenu,
                                onDismissRequest = { showBatchMoveFolderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("未分组") },
                                    onClick = {
                                        showBatchMoveFolderMenu = false
                                        viewModel.batchMoveSelectedToFolder(null)
                                    }
                                )
                                uiState.folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text("📁 ${folder.name}") },
                                        onClick = {
                                            showBatchMoveFolderMenu = false
                                            viewModel.batchMoveSelectedToFolder(folder.id)
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showBatchDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "批量删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                // 标准 TopBar
                TopAppBar(
                    title = {
                        FolderDropdownSelector(
                            currentFilter = uiState.currentFilter,
                            folders = uiState.folders,
                            onFilterSelect = { viewModel.onFolderFilterSelected(it) },
                            onOpenRecycleBin = onNavigateToRecycleBin,
                            onOpenFolderManager = { showFolderManagerDialog = true }
                        )
                    },
                    actions = {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "更多")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("排序设置") },
                                onClick = {
                                    showTopMenu = false
                                    showSortDialog = true
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Sort, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("导入笔记 (.txt / .md)") },
                                onClick = {
                                    showTopMenu = false
                                    importLauncher.launch("*/*")
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 实时搜索框
            NoteSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) }
            )

            // 笔记列表区域与悬浮 FAB
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.notes.isEmpty()) {
                    // 空状态引导
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "没有匹配的笔记" else "暂无笔记，点击下方加号新建",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.notes,
                            key = { it.id }
                        ) { note ->
                            val folderName = uiState.folders.find { it.id == note.folderId }?.name
                            val isSelected = uiState.selectedNoteIds.contains(note.id)

                            NoteItem(
                                note = note,
                                searchQuery = uiState.searchQuery,
                                folderName = folderName,
                                folders = uiState.folders,
                                isSelected = isSelected,
                                isMultiSelectMode = uiState.isMultiSelectMode,
                                onClick = { onNavigateToEditor(note.id) },
                                onLongClick = { viewModel.toggleSelectNote(note.id) },
                                onTogglePin = { viewModel.togglePinStatus(note) },
                                onDelete = { noteToDelete = note },
                                onMoveToFolder = { newFolderId ->
                                    viewModel.batchMoveSelectedToFolder(newFolderId)
                                }
                            )
                        }
                    }
                }

                // FAB 新建按钮，固定在笔记列表区域右下角
                FloatingActionButton(
                    onClick = { onNavigateToEditor(-1L) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("create_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建笔记"
                    )
                }
            }
        }
    }

    // 排序弹窗
    if (showSortDialog) {
        SortDialog(
            currentSortOption = uiState.sortOption,
            onDismiss = { showSortDialog = false },
            onConfirm = { field, direction ->
                viewModel.onSortOptionChanged(field, direction)
            }
        )
    }

    // 新建分组弹窗
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { folderName ->
                viewModel.createFolder(folderName)
            }
        )
    }

    // 分组管理弹窗
    if (showFolderManagerDialog) {
        FolderManagerDialog(
            folders = uiState.folders,
            onDismiss = { showFolderManagerDialog = false },
            onCreateFolderClick = { showCreateFolderDialog = true },
            onRenameFolder = { folder, newName -> viewModel.updateFolder(folder, newName) },
            onDeleteFolder = { folder -> viewModel.deleteFolder(folder) }
        )
    }

    // 单个笔记删除二次确认
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("移动至回收站？") },
            text = { Text("笔记“${note.title.ifBlank { "无标题笔记" }}”将被移入回收站，30天后自动清除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.softDeleteNote(note.id)
                        noteToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_note")
                ) {
                    Text("移动", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 批量删除二次确认
    if (showBatchDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            title = { Text("移入回收站") },
            text = { Text("确定将选中的 ${uiState.selectedNoteIds.size} 条笔记移入回收站吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.batchSoftDeleteSelected()
                        showBatchDeleteConfirmDialog = false
                    }
                ) {
                    Text("移入回收站", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
