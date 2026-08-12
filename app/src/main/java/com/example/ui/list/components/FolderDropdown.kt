package com.example.ui.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder
import com.example.ui.list.FolderFilter

@Composable
fun FolderDropdownSelector(
    currentFilter: FolderFilter,
    folders: List<Folder>,
    onFilterSelect: (FolderFilter) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenFolderManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val currentTitle = when (currentFilter) {
        is FolderFilter.All -> "全部笔记"
        is FolderFilter.Uncategorized -> "未分组"
        is FolderFilter.SelectedFolder -> currentFilter.folder.name
    }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("folder_dropdown_selector")
        ) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择分组",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("全部笔记") },
                onClick = {
                    expanded = false
                    onFilterSelect(FolderFilter.All)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Notes, contentDescription = null)
                }
            )

            DropdownMenuItem(
                text = { Text("未分组") },
                onClick = {
                    expanded = false
                    onFilterSelect(FolderFilter.Uncategorized)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = null)
                }
            )

            if (folders.isNotEmpty()) {
                HorizontalDivider()
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.name) },
                        onClick = {
                            expanded = false
                            onFilterSelect(FolderFilter.SelectedFolder(folder))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                        }
                    )
                }
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("分组管理") },
                onClick = {
                    expanded = false
                    onOpenFolderManager()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
            )

            DropdownMenuItem(
                text = { Text("回收站", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onOpenRecycleBin()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}
