package org.abdat.inventory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.theme.AppTheme

enum class StockStatusType {
    IN_STOCK,
    OUT_OF_STOCK,
    URGENT_DEBT,
    LOW_STOCK,
    ARCHIVED,
    OBSOLETE
}

enum class StatusType {
    SUCCESS,
    WARNING,
    URGENT,
    NEUTRAL
}

@Composable
fun StatusChip(
    type: StatusType,
    text: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (type) {
        StatusType.SUCCESS -> Pair(AppTheme.colors.statusSuccessContainer, AppTheme.colors.onStatusSuccess)
        StatusType.WARNING -> Pair(AppTheme.colors.statusWarningContainer, AppTheme.colors.onStatusWarning)
        StatusType.URGENT -> Pair(AppTheme.colors.statusUrgentContainer, AppTheme.colors.onStatusUrgent)
        StatusType.NEUTRAL -> Pair(MaterialTheme.colorScheme.surfaceContainerHigh, AppTheme.colors.textSecondary)
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(9999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.sp
        )
    }
}

@Composable
fun StatusChip(
    status: StockStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val (backgroundColor, textColor, defaultLabel) = when (status) {
        StockStatusType.IN_STOCK -> Triple(
            AppTheme.colors.statusSuccessContainer,
            AppTheme.colors.onStatusSuccess,
            "EN STOCK"
        )
        StockStatusType.OUT_OF_STOCK -> Triple(
            AppTheme.colors.statusUrgentContainer,
            AppTheme.colors.onStatusUrgent,
            "RUPTURE"
        )
        StockStatusType.URGENT_DEBT -> Triple(
            AppTheme.colors.statusWarningContainer,
            AppTheme.colors.onStatusWarning,
            "DETTE URGENTE"
        )
        StockStatusType.LOW_STOCK -> Triple(
            AppTheme.colors.statusWarningContainer,
            AppTheme.colors.onStatusWarning,
            "STOCK FAIBLE"
        )
        StockStatusType.ARCHIVED -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            AppTheme.colors.textSecondary,
            "ARCHIVÉ"
        )
        StockStatusType.OBSOLETE -> Triple(
            AppTheme.colors.statusUrgentContainer,
            AppTheme.colors.onStatusUrgent,
            "OBSOLÈTE"
        )
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(9999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = customLabel ?: defaultLabel,
            color = textColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.sp
        )
    }
}
