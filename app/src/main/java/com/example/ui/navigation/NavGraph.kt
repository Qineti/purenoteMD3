package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.data.repository.NoteRepository
import com.example.ui.editor.NoteEditorScreen
import com.example.ui.editor.NoteEditorViewModel
import com.example.ui.list.NoteListScreen
import com.example.ui.list.NoteListViewModel
import com.example.ui.recyclebin.NoteDetailReadOnlyScreen
import com.example.ui.recyclebin.RecycleBinScreen
import com.example.ui.recyclebin.RecycleBinViewModel

sealed class Screen(val route: String) {
    object NoteList : Screen("note_list")
    object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long) = "note_editor/$noteId"
    }
    object RecycleBin : Screen("recycle_bin")
    object NoteDetailReadOnly : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
}

@Composable
fun NoteAppNavGraph(
    navController: NavHostController,
    repository: NoteRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NoteList.route
    ) {
        // 主界面（NoteListScreen）
        composable(Screen.NoteList.route) {
            val listViewModel: NoteListViewModel = viewModel(
                factory = NoteListViewModel.Factory(repository)
            )

            NoteListScreen(
                viewModel = listViewModel,
                onNavigateToEditor = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onNavigateToRecycleBin = {
                    navController.navigate(Screen.RecycleBin.route)
                }
            )
        }

        // 笔记编辑页（NoteEditorScreen）
        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val editorViewModel: NoteEditorViewModel = viewModel(
                factory = NoteEditorViewModel.Factory(repository, noteId)
            )

            NoteEditorScreen(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 回收站（RecycleBinScreen）
        composable(Screen.RecycleBin.route) {
            val recycleBinViewModel: RecycleBinViewModel = viewModel(
                factory = RecycleBinViewModel.Factory(repository)
            )

            RecycleBinScreen(
                viewModel = recycleBinViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetailReadOnly = { noteId ->
                    navController.navigate(Screen.NoteDetailReadOnly.createRoute(noteId))
                }
            )
        }

        // 只读详情页
        composable(
            route = Screen.NoteDetailReadOnly.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L

            NoteDetailReadOnlyScreen(
                noteId = noteId,
                repository = repository,
                onBack = { navController.popBackStack() },
                onRestored = {
                    // 恢复后返回回收站列表
                    navController.popBackStack()
                }
            )
        }
    }
}
