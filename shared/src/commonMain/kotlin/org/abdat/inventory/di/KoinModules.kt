package org.abdat.inventory.di

import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.database.createDatabase
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.suppliers.SuppliersViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        val driverFactory = get<DatabaseDriverFactory>()
        val db = createDatabase(driverFactory)
        seedInitialDataIfEmpty(db)
        db
    }
}

val viewModelModule = module {
    single { InventoryViewModel(get()) }
    single { SuppliersViewModel(get()) }
    single { OrdersViewModel(get()) }
}

val appModule = module {
    includes(databaseModule, viewModelModule)
}

fun initKoin(appDeclaration: (KoinApplication.() -> Unit)? = null): KoinApplication =
    startKoin {
        appDeclaration?.invoke(this)
        modules(appModule)
    }
