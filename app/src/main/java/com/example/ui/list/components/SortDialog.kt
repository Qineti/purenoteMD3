package com.example.ui.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.SortOption

@Composable
fun SortDialog(
    currentSortOption: SortOption,
    onDismiss: () -> Unit,
    onConfirm: (SortField, SortDirection) -> Unit
) {
    var selectedField by remember { mutableStateOf(currentSortOption.field) }
    var selectedDirection by remember { mutableStateOf(currentSortOption.direction) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("笔记排序") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "排序字段",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                SortFieldRow("更新时间", SortField.UPDATED_AT, selectedField) { selectedField = it }
                SortFieldRow("创建时间", SortField.CREATED_AT, selectedField) { selectedField = it }
                SortFieldRow("标题名称", SortField.TITLE, selectedField) { selectedField = it }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "排序顺序",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                SortDirectionRow("降序（从新到旧 / Z-A）", SortDirection.DESC, selectedDirection) { selectedDirection = it }
                SortDirectionRow("升序（从旧到新 / A-Z）", SortDirection.ASC, selectedDirection) { selectedDirection = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedField, selectedDirection)
                    onDismiss()
                },
                modifier = Modifier.testTag("sort_dialog_confirm")
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SortFieldRow(
    label: String,
    field: SortField,
    currentField: SortField,
    onSelect: (SortField) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(field) }
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = (field == currentField),
            onClick = { onSelect(field) }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SortDirectionRow(
    label: String,
    direction: SortDirection,
    currentDirection: SortDirection,
    onSelect: (SortDirection) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(direction) }
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = (direction == currentDirection),
            onClick = { onSelect(direction) }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
