package org.abdat.inventory.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Primary & Brand Tokens
val Primary = Color(0xFF004AC6)
val PrimaryContainer = Color(0xFF2563EB)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryContainer = Color(0xFFEEEFFF)
val InversePrimary = Color(0xFFB4C5FF)

// Secondary Tokens
val Secondary = Color(0xFF505F76)
val SecondaryContainer = Color(0xFFD0E1FB)
val OnSecondary = Color(0xFFFFFFFF)
val OnSecondaryContainer = Color(0xFF54647A)

// Surfaces & Backgrounds
val Background = Color(0xFFFAF8FF)
val BackgroundSlate = Color(0xFFF8FAFC)
val OnBackground = Color(0xFF191B23)
val Surface = Color(0xFFFAF8FF)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceDim = Color(0xFFD9D9E5)
val SurfaceBright = Color(0xFFFAF8FF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF3F3FE)
val SurfaceContainer = Color(0xFFEDEDF9)
val SurfaceContainerHigh = Color(0xFFE7E7F3)
val SurfaceContainerHighest = Color(0xFFE1E2ED)
val OnSurface = Color(0xFF191B23)
val OnSurfaceVariant = Color(0xFF434655)
val InverseSurface = Color(0xFF2E3039)
val InverseOnSurface = Color(0xFFF0F0FB)

// Outlines
val Outline = Color(0xFF737686)
val OutlineVariant = Color(0xFFC3C6D7)

// Text tokens
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

// Semantic & Status Tokens
val StatusSuccess = Color(0xFF10B981)
val StatusSuccessContainer = Color(0xFFE6F8F0)
val OnStatusSuccess = Color(0xFF065F46)

val StatusWarning = Color(0xFFF59E0B)
val StatusWarningContainer = Color(0xFFFEF3C7)
val OnStatusWarning = Color(0xFF92400E)

val StatusUrgent = Color(0xFFEF4444)
val StatusUrgentContainer = Color(0xFFFEE2E2)
val OnStatusUrgent = Color(0xFF991B1B)

val Error = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF93000A)

@Immutable
data class StockCustomColors(
    val statusSuccess: Color = StatusSuccess,
    val statusSuccessContainer: Color = StatusSuccessContainer,
    val onStatusSuccess: Color = OnStatusSuccess,
    val statusWarning: Color = StatusWarning,
    val statusWarningContainer: Color = StatusWarningContainer,
    val onStatusWarning: Color = OnStatusWarning,
    val statusUrgent: Color = StatusUrgent,
    val statusUrgentContainer: Color = StatusUrgentContainer,
    val onStatusUrgent: Color = OnStatusUrgent,
    val backgroundSlate: Color = BackgroundSlate,
    val surfaceWhite: Color = SurfaceWhite,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary
)
