package org.abdat.inventory

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.theme.AppTypography
import org.abdat.inventory.theme.BackgroundSlate
import org.abdat.inventory.theme.LightColorScheme
import org.abdat.inventory.theme.Primary
import org.abdat.inventory.theme.StatusSuccess
import org.abdat.inventory.theme.StatusUrgent
import org.abdat.inventory.theme.StatusWarning
import org.abdat.inventory.theme.StockCustomColors
import org.abdat.inventory.theme.StockSpacing
import org.abdat.inventory.theme.TextPrimary
import org.abdat.inventory.theme.TextSecondary
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTest {

    @Test
    fun testLightColorSchemeTokens() {
        assertEquals(Primary, LightColorScheme.primary)
    }

    @Test
    fun testCustomStatusColorTokens() {
        val colors = StockCustomColors()
        assertEquals(StatusSuccess, colors.statusSuccess)
        assertEquals(StatusWarning, colors.statusWarning)
        assertEquals(StatusUrgent, colors.statusUrgent)
        assertEquals(BackgroundSlate, colors.backgroundSlate)
        assertEquals(TextPrimary, colors.textPrimary)
        assertEquals(TextSecondary, colors.textSecondary)
    }

    @Test
    fun testSpacingTokens() {
        val spacing = StockSpacing()
        assertEquals(4.dp, spacing.xxs)
        assertEquals(8.dp, spacing.xs)
        assertEquals(12.dp, spacing.sm)
        assertEquals(16.dp, spacing.md)
        assertEquals(24.dp, spacing.lg)
        assertEquals(32.dp, spacing.xl)
        assertEquals(48.dp, spacing.xxl)
        assertEquals(256.dp, spacing.sidebarWidth)
        assertEquals(10.dp, spacing.tableCellY)
        assertEquals(16.dp, spacing.tableCellX)
    }

    @Test
    fun testTypographyTokens() {
        assertEquals(30.sp, AppTypography.headlineLarge.fontSize)
        assertEquals(22.sp, AppTypography.headlineMedium.fontSize)
        assertEquals(18.sp, AppTypography.headlineSmall.fontSize)
        assertEquals(16.sp, AppTypography.titleMedium.fontSize)
        assertEquals(14.sp, AppTypography.bodyMedium.fontSize)
        assertEquals(11.sp, AppTypography.labelSmall.fontSize)
    }
}
