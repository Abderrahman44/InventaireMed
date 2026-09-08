package org.abdat.inventory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    URGENT_DEBT
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
