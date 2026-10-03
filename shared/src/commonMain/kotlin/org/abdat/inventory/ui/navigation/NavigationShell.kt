package org.abdat.inventory.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.theme.AppLanguage
import org.abdat.inventory.theme.AppStrings
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.theme.ThemeMode

enum class AppScreenRoute(val fallbackTitle: String, val icon: ImageVector, val subtitle: String) {
    INVENTORY("Gestion de Stock", Icons.Default.Inventory2, "Catalogue général & valorisation"),
    SUPPLIERS("Fournisseurs", Icons.Default.Business, "Conventions-cadres & quotas"),
    ORDERS("Commandes", Icons.AutoMirrored.Filled.ReceiptLong, "Bons de commande & réceptions"),
    DELIVERIES("Livraisons", Icons.Default.LocalShipping, "Réceptions & entrées en stock"),
    SORTIES("Sorties Matériel", Icons.AutoMirrored.Filled.ExitToApp, "Bons de sortie & consommation interne"),
    DECHARGES("Dons & Donations", Icons.Default.CardGiftcard, "Donations & entrées sans incidence budgétaire"),
    SERVICES("Services & Locaux", Icons.Default.Domain, "Structure organisationnelle"),
    PERSONNEL("Personnel", Icons.Default.Badge, "Répertoire des collaborateurs & chefs de service"),
    CONFIG("Paramètres & Config", Icons.Default.Settings, "Configuration dynamique & profil"),
    DASHBOARD("Consommation", Icons.Default.Dashboard, "Suivi de consommation & valorisation")
}

fun AppScreenRoute.localizedTitle(strings: AppStrings): String = when (this) {
    AppScreenRoute.INVENTORY -> strings.navInventory
    AppScreenRoute.SUPPLIERS -> strings.navSuppliers
    AppScreenRoute.ORDERS -> strings.navOrders
    AppScreenRoute.DELIVERIES -> strings.navDeliveries
    AppScreenRoute.SORTIES -> strings.navSorties
    AppScreenRoute.DECHARGES -> strings.navDecharges
    AppScreenRoute.SERVICES -> strings.navServices
    AppScreenRoute.PERSONNEL -> strings.navPersonnel
    AppScreenRoute.CONFIG -> strings.navConfig
    AppScreenRoute.DASHBOARD -> strings.navDashboard
}

@Composable
fun MainNavigationShell(
    currentRoute: AppScreenRoute,
    onNavigate: (AppScreenRoute) -> Unit,
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onToggleTheme: () -> Unit = {},
    language: AppLanguage = AppLanguage.FR,
    onToggleLanguage: () -> Unit = {},
    userFullName: String = "Dr. Amina Mansouri",
    userRole: String = "Chef Pharmacien / Responsable Stock",
    content: @Composable () -> Unit
) {
    val strings = AppTheme.strings

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
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section (Brand, Status & Toggles)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        }

                        // Theme & Language Switchers in Header
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onToggleLanguage,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Toggle Language",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Depot indicator badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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
                                text = "Plateforme Logistique • " + if (language == AppLanguage.FR) "FR" else "EN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 1: Opérations
                    Text(
                        text = strings.navOperations,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val operationsRoutes = listOf(
                        AppScreenRoute.DASHBOARD,
                        AppScreenRoute.INVENTORY,
                        AppScreenRoute.SUPPLIERS,
                        AppScreenRoute.ORDERS,
                        AppScreenRoute.DELIVERIES,
                        AppScreenRoute.SORTIES,
                        AppScreenRoute.DECHARGES
                    )

                    operationsRoutes.forEach { route ->
                        val isSelected = route == currentRoute
                        SidebarNavItem(
                            title = route.localizedTitle(strings),
                            icon = route.icon,
                            isSelected = isSelected,
                            onClick = { onNavigate(route) }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 2: Configuration
                    Text(
                        text = strings.navConfiguration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val configRoutes = listOf(
                        AppScreenRoute.SERVICES,
                        AppScreenRoute.PERSONNEL,
                        AppScreenRoute.CONFIG
                    )

                    configRoutes.forEach { route ->
                        val isSelected = route == currentRoute
                        SidebarNavItem(
                            title = route.localizedTitle(strings),
                            icon = route.icon,
                            isSelected = isSelected,
                            onClick = { onNavigate(route) }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }

                // Footer Profile / User details
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onNavigate(AppScreenRoute.CONFIG) }
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
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userFullName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = userRole,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary,
                                maxLines = 1
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
    icon: ImageVector,
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
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}
