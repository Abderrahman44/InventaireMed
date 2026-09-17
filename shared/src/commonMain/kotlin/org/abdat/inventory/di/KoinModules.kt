package org.abdat.inventory.di

import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.DatabaseDriverFactory
import org.abdat.inventory.database.createDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import org.abdat.inventory.ui.decharges.DechargeViewModel
import org.abdat.inventory.ui.deliveries.DeliveriesViewModel
import org.abdat.inventory.ui.inventory.ArticleDetailViewModel
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.personnel.PersonnelViewModel
import org.abdat.inventory.ui.services.ServicesViewModel
import org.abdat.inventory.ui.sorties.SortiesViewModel
import org.abdat.inventory.ui.suppliers.SuppliersViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        val driverFactory = get<DatabaseDriverFactory>()
        val db = createDatabase(driverFactory)
        // Baseline taxonomy and legacy-label reconciliation runs on every startup.
        ensureCategoryTaxonomy(db)
        db
    }
}

val repositoryModule = module {
    single { StockRepository(get(), get()) }
    single { CategoryRepository(get(), get()) }
}

val viewModelModule = module {
    single { InventoryViewModel(get(), get(), get()) }
    single { SuppliersViewModel(get()) }
    single { OrdersViewModel(get()) }
    single { DeliveriesViewModel(get()) }
    single { SortiesViewModel(get(), get()) }
    single { ServicesViewModel(get()) }
    single { DechargeViewModel(get()) }
    single { PersonnelViewModel(get()) }
    single { DynamicConfigViewModel(get(), get()) }
    single { ArticleDetailViewModel(get(), get()) }
}

val appModule = module {
    includes(databaseModule, repositoryModule, viewModelModule)
}

fun initKoin(appDeclaration: (KoinApplication.() -> Unit)? = null): KoinApplication =
    startKoin {
        appDeclaration?.invoke(this)
        modules(appModule)
    }
