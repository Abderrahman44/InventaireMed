package org.abdat.inventory.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.theme.AppTheme

enum class AppScreenRoute(val title: String, val iconSymbol: String, val subtitle: String) {
    INVENTORY("Gestion de Stock", "📦", "Catalogue général & valorisation"),
    SUPPLIERS("Fournisseurs", "🏢", "Conventions-cadres & quotas"),
    ORDERS("Commandes", "📋", "Bons de commande & réceptions"),
    DASHBOARD("Tableau de Bord", "📊", "Aperçu général & indicateurs")
}

@Composable
fun MainNavigationShell(
    currentRoute: AppScreenRoute,
    onNavigate: (AppScreenRoute) -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
    ) {
        // Left Desktop Sidebar
        Surface(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
            color = AppTheme.colors.surfaceWhite,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Brand & Platform Info
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IM",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "InventaireMed",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Gestion Industrielle & Médicale",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Warehouse / Depot indicator badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AppTheme.colors.statusSuccess, CircleShape)
                            )
                            Text(
                                text = "Plateforme A • Quai 03",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nav Menu Items
                    Text(
                        text = "MENU PRINCIPAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        AppScreenRoute.INVENTORY,
                        AppScreenRoute.SUPPLIERS,
                        AppScreenRoute.ORDERS,
                        AppScreenRoute.DASHBOARD
                    ).forEach { route ->
                        val isSelected = route == currentRoute
                        SidebarNavItem(
                            title = route.title,
                            iconSymbol = route.iconSymbol,
                            isSelected = isSelected,
                            onClick = { onNavigate(route) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Footer Profile / User details
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 14.sp)
                        }
                        Column {
                            Text(
                                text = "Responsable Stock",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Pharmacie Centrale",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Main Workspace Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.backgroundSlate)
        ) {
            content()
        }
    }
}

@Composable
private fun SidebarNavItem(
    title: String,
    iconSymbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        AppTheme.colors.textPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = iconSymbol,
            fontSize = 15.sp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}
