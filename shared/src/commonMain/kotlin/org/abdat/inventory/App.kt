package org.abdat.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.inventory.InventoryScreen
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.navigation.AppScreenRoute
import org.abdat.inventory.ui.navigation.MainNavigationShell
import org.abdat.inventory.ui.orders.OrdersScreen
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.suppliers.SuppliersScreen
import org.abdat.inventory.ui.suppliers.SuppliersViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    AppTheme {
        var currentRoute by remember { mutableStateOf(AppScreenRoute.INVENTORY) }

        val inventoryViewModel: InventoryViewModel = koinInject()
        val suppliersViewModel: SuppliersViewModel = koinInject()
        val ordersViewModel: OrdersViewModel = koinInject()

        MainNavigationShell(
            currentRoute = currentRoute,
            onNavigate = { currentRoute = it }
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
                    AppScreenRoute.DASHBOARD -> {
                        // Overview defaults to the Master Inventory hub
                        InventoryScreen(viewModel = inventoryViewModel)
                    }
                }
            }
        }
    }
}
