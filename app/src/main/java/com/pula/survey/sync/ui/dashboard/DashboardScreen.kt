package com.pula.survey.sync.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pula.survey.sync.domain.model.TerminationReason
import com.pula.survey.sync.ui.components.StatCard
import com.pula.survey.sync.ui.theme.FailedFatalColor
import com.pula.survey.sync.ui.theme.FailedRetryableColor
import com.pula.survey.sync.ui.theme.PendingColor
import com.pula.survey.sync.ui.theme.SyncedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onViewAll: () -> Unit,
    onSyncClick: () -> Unit
) {
    val stats by viewModel.storageStats.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastResult by viewModel.lastSyncResult.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PulaSurveySync", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSyncClick,
                icon = {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                    }
                },
                text = { Text(if (isSyncing) "Syncing..." else "Sync Now") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("PENDING", stats.pendingCount, PendingColor, Modifier.weight(1f))
                StatCard("SYNCED", stats.syncedCount, SyncedColor, Modifier.weight(1f))
                StatCard("FAILED", stats.failedRetryableCount + stats.failedFatalCount,
                    FailedRetryableColor, Modifier.weight(1f))
            }

            // Last sync result
            lastResult?.let { result ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Last Sync Result", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("${result.successful.size} uploaded, ${result.failed.size} failed, ${result.pending.size} skipped")
                        if (result.terminationReason == TerminationReason.NETWORK_UNAVAILABLE) {
                            Text(
                                "Stopped early: Network appears down",
                                color = FailedFatalColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Storage info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            "Storage: ${formatBytes(stats.totalAttachmentSizeBytes)} (${stats.totalResponses} surveys)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (stats.syncedCount > 0) {
                            Text(
                                "${stats.syncedCount} synced surveys can be cleaned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.generateTestData() },
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Dataset, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (isGenerating) " Generating..." else " Generate 10 Surveys")
                }
            }

            Button(
                onClick = onViewAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View All Surveys")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
