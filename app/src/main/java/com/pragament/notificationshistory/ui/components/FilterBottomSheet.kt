package com.pragament.notificationshistory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pragament.notificationshistory.data.dao.AppInfo

enum class StatusFilter {
    ALL, READ, UNREAD, SNOOZED
}

data class FilterState(
    val selectedApps: Set<String> = emptySet(),
    val statusFilter: StatusFilter = StatusFilter.ALL
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    availableApps: List<AppInfo>,
    currentFilter: FilterState,
    onApplyFilter: (FilterState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statusFilter by remember(currentFilter) { mutableStateOf(currentFilter.statusFilter) }
    val selectedApps = remember(currentFilter) { 
        mutableStateListOf<String>().apply { 
            addAll(currentFilter.selectedApps) 
        } 
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Filter Notifications",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Filter
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { 
                            Text(
                                when (status) {
                                    StatusFilter.ALL -> "All"
                                    StatusFilter.READ -> "Read"
                                    StatusFilter.UNREAD -> "Unread"
                                    StatusFilter.SNOOZED -> "Snoozed"
                                }
                            ) 
                        },
                        leadingIcon = if (statusFilter == status) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.height(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Apps Filter
            Text(
                text = "Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (availableApps.isEmpty()) {
                Text(
                    text = "No apps with notifications yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(availableApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedApps.contains(app.packageName),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedApps.add(app.packageName)
                                    } else {
                                        selectedApps.remove(app.packageName)
                                    }
                                }
                            )
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        statusFilter = StatusFilter.ALL
                        selectedApps.clear()
                    }
                ) {
                    Text("Reset")
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = {
                        onApplyFilter(
                            FilterState(
                                selectedApps = selectedApps.toSet(),
                                statusFilter = statusFilter
                            )
                        )
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
