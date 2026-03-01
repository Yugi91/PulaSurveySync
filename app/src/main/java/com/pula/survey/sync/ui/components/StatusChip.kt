package com.pula.survey.sync.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.ui.theme.FailedFatalBg
import com.pula.survey.sync.ui.theme.FailedFatalColor
import com.pula.survey.sync.ui.theme.FailedRetryableBg
import com.pula.survey.sync.ui.theme.FailedRetryableColor
import com.pula.survey.sync.ui.theme.PendingBg
import com.pula.survey.sync.ui.theme.PendingColor
import com.pula.survey.sync.ui.theme.SyncedBg
import com.pula.survey.sync.ui.theme.SyncedColor

@Composable
fun StatusChip(status: SyncStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        SyncStatus.PENDING -> Triple(PendingBg, PendingColor, "PENDING")
        SyncStatus.SYNCED -> Triple(SyncedBg, SyncedColor, "SYNCED")
        SyncStatus.FAILED_RETRYABLE -> Triple(FailedRetryableBg, FailedRetryableColor, "RETRY")
        SyncStatus.FAILED_FATAL -> Triple(FailedFatalBg, FailedFatalColor, "FATAL")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun statusColor(status: SyncStatus): Color = when (status) {
    SyncStatus.PENDING -> PendingColor
    SyncStatus.SYNCED -> SyncedColor
    SyncStatus.FAILED_RETRYABLE -> FailedRetryableColor
    SyncStatus.FAILED_FATAL -> FailedFatalColor
}
