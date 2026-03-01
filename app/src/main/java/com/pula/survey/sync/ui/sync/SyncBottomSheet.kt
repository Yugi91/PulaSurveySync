package com.pula.survey.sync.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pula.survey.sync.domain.model.TerminationReason
import com.pula.survey.sync.ui.theme.FailedFatalColor
import com.pula.survey.sync.ui.theme.PendingColor
import com.pula.survey.sync.ui.theme.SyncedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncBottomSheet(
    viewModel: SyncViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.startSync()
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is SyncUiState.Idle -> {
                    Text("Preparing sync...", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                }

                is SyncUiState.InProgress -> {
                    Text(
                        "Syncing Surveys...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${state.current} / ${state.total}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.items) { item ->
                            SyncItemRow(item)
                        }
                    }
                }

                is SyncUiState.Result -> {
                    val result = state.result
                    Text(
                        "Sync Complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    if (result.successful.isNotEmpty()) {
                        ResultRow(Icons.Default.CheckCircle, SyncedColor, "${result.successful.size} uploaded successfully")
                    }
                    if (result.failed.isNotEmpty()) {
                        ResultRow(Icons.Default.Error, FailedFatalColor, "${result.failed.size} failed")
                    }
                    if (result.pending.isNotEmpty()) {
                        ResultRow(Icons.Default.HourglassEmpty, PendingColor, "${result.pending.size} skipped")
                    }

                    if (result.terminationReason == TerminationReason.NETWORK_UNAVAILABLE) {
                        Text(
                            "Stopped early: Network appears down",
                            color = FailedFatalColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (result.terminationReason == TerminationReason.EMPTY_QUEUE) {
                        Text(
                            "No surveys to sync",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.reset()
                        onDismiss()
                    }) {
                        Text("Done")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SyncItemRow(item: SyncItemUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (icon, color) = when (item.state) {
            SyncItemState.WAITING -> Icons.Default.HourglassEmpty to MaterialTheme.colorScheme.outline
            SyncItemState.UPLOADING -> Icons.Default.Sync to MaterialTheme.colorScheme.primary
            SyncItemState.SUCCESS -> Icons.Default.CheckCircle to SyncedColor
            SyncItemState.FAILED -> Icons.Default.Error to FailedFatalColor
        }
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = when (item.state) {
                SyncItemState.WAITING -> "Waiting"
                SyncItemState.UPLOADING -> "Uploading..."
                SyncItemState.SUCCESS -> "Done"
                SyncItemState.FAILED -> "Failed"
            },
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun ResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
