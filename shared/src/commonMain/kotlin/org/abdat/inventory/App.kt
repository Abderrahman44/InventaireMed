package org.abdat.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.abdat.inventory.theme.AppLanguage
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.theme.ThemeMode
import org.abdat.inventory.ui.config.DynamicConfigScreen
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import org.abdat.inventory.ui.decharges.DechargeScreen
import org.abdat.inventory.ui.decharges.DechargeViewModel
import org.abdat.inventory.ui.deliveries.DeliveriesScreen
import org.abdat.inventory.ui.deliveries.DeliveriesViewModel
import org.abdat.inventory.ui.inventory.InventoryScreen
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.navigation.AppScreenRoute
import org.abdat.inventory.ui.navigation.MainNavigationShell
import org.abdat.inventory.ui.orders.OrdersScreen
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.personnel.PersonnelScreen
import org.abdat.inventory.ui.personnel.PersonnelViewModel
import org.abdat.inventory.ui.services.ServicesScreen
import org.abdat.inventory.ui.services.ServicesViewModel
import org.abdat.inventory.ui.sorties.SortiesScreen
import org.abdat.inventory.ui.sorties.SortiesViewModel
import org.abdat.inventory.ui.suppliers.SuppliersScreen
import org.abdat.inventory.ui.suppliers.SuppliersViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }
    var language by remember { mutableStateOf(AppLanguage.FR) }

    val inventoryViewModel: InventoryViewModel = koinInject()
    val suppliersViewModel: SuppliersViewModel = koinInject()
    val ordersViewModel: OrdersViewModel = koinInject()
    val deliveriesViewModel: DeliveriesViewModel = koinInject()
    val sortiesViewModel: SortiesViewModel = koinInject()
    val servicesViewModel: ServicesViewModel = koinInject()
    val dechargeViewModel: DechargeViewModel = koinInject()
    val personnelViewModel: PersonnelViewModel = koinInject()
    val dynamicConfigViewModel: DynamicConfigViewModel = koinInject()

    val configState by dynamicConfigViewModel.uiState.collectAsState()

    AppTheme(themeMode = themeMode, language = language) {
        var currentRoute by remember { mutableStateOf(AppScreenRoute.INVENTORY) }

        MainNavigationShell(
            currentRoute = currentRoute,
            onNavigate = { currentRoute = it },
            themeMode = themeMode,
            onToggleTheme = {
                themeMode = if (themeMode == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
            },
            language = language,
            onToggleLanguage = {
                language = if (language == AppLanguage.FR) AppLanguage.EN else AppLanguage.FR
            },
            userFullName = configState.userProfile.fullName,
            userRole = configState.userProfile.role
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentRoute) {
                    AppScreenRoute.INVENTORY -> {
                        InventoryScreen(viewModel = inventoryViewModel)
                    }
                    AppScreenRoute.SUPPLIERS -> {
                        SuppliersScreen(viewModel = suppliersViewModel)
                    }
                    AppScreenRoute.ORDERS -> {
                        OrdersScreen(viewModel = ordersViewModel)
                    }
                    AppScreenRoute.DELIVERIES -> {
                        DeliveriesScreen(viewModel = deliveriesViewModel)
                    }
                    AppScreenRoute.SORTIES -> {
                        SortiesScreen(viewModel = sortiesViewModel)
                    }
                    AppScreenRoute.DECHARGES -> {
                        DechargeScreen(viewModel = dechargeViewModel)
                    }
                    AppScreenRoute.SERVICES -> {
                        ServicesScreen(viewModel = servicesViewModel)
                    }
                    AppScreenRoute.PERSONNEL -> {
                        PersonnelScreen(viewModel = personnelViewModel)
                    }
                    AppScreenRoute.CONFIG -> {
                        DynamicConfigScreen(viewModel = dynamicConfigViewModel)
                    }
                    AppScreenRoute.DASHBOARD -> {
                        // Overview defaults to the Master Inventory hub
                        InventoryScreen(viewModel = inventoryViewModel)
                    }
                }
            }
        }
    }
}
